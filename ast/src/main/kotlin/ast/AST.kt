package cnc.ast

import ast.ExpressionVisitor
import cnc.token.Token
import cnc.token.TokenDefinition

// =============================================================================
// Expressions
// =============================================================================

sealed interface Expression {
    fun <R> accept(visitor: ExpressionVisitor<R>): R
}
data class NumberLiteral(val value: Double) : Expression {
    override fun <R> accept(visitor: ExpressionVisitor<R>) = visitor.visit(this)
}
data class StringLiteral(val value: String) : Expression {
    override fun <R> accept(visitor: ExpressionVisitor<R>) = visitor.visit(this)
}
data class Identifier(val name: String) : Expression {
    override fun <R> accept(visitor: ExpressionVisitor<R>) = visitor.visit(this)
}
data class BinaryExpression(
    val left: Expression,
    val operator: String,
    val right: Expression
) : Expression {
    override fun <R> accept(visitor: ExpressionVisitor<R>) = visitor.visit(this)
}
data class UnaryExpression(
    val operator: String,
    val operand: Expression
) : Expression {
    override fun <R> accept(visitor: ExpressionVisitor<R>) = visitor.visit(this)
}

// =============================================================================
// Operator definitions
// =============================================================================

enum class Associativity { LEFT, RIGHT }

data class OperatorDef(
    val definition: TokenDefinition,
    val precedence: Int,
    val associativity: Associativity = Associativity.LEFT
)

data class PrefixOperatorDef(
    val definition: TokenDefinition,
    val precedence: Int
)

// =============================================================================
// ExpressionBuilder — Pratt parser for expressions
// =============================================================================

class ExpressionBuilder(
    private val recipes: Map<TokenDefinition, (Token) -> Expression>,
    private val operators: List<OperatorDef> = emptyList(),
    private val prefixOperators: List<PrefixOperatorDef> = emptyList(),
    private val groupOpen: TokenDefinition? = null,
    private val groupClose: TokenDefinition? = null
) {

    fun build(token: Token): Expression {
        val (_, builder) = recipes.entries.firstOrNull { (definition, _) ->
            definition.match(token.text)
        } ?: error("No recipe matches token: '${token.text}'")
        return builder(token)
    }

    fun build(tokens: List<Token>): Expression {
        if (tokens.isEmpty()) error("Cannot build expression from empty token list")
        if (operators.isEmpty() || tokens.size == 1) {
            return build(tokens.first())
        }
        val stream = TokenStream(tokens)
        val result = parseExpression(stream, 0)
        if (stream.hasNext()) {
            error("Unexpected token '${stream.peek().text}' after expression")
        }
        return result
    }

    private fun parseExpression(stream: TokenStream, minPrecedence: Int): Expression {
        var left = parseAtom(stream)

        while (stream.hasNext()) {
            val opToken = stream.peek()
            val opDef = findOperator(opToken) ?: break
            if (opDef.precedence < minPrecedence) break

            stream.advance()
            val nextMinPrecedence = when (opDef.associativity) {
                Associativity.LEFT -> opDef.precedence + 1
                Associativity.RIGHT -> opDef.precedence
            }
            val right = parseExpression(stream, nextMinPrecedence)
            left = BinaryExpression(left, opToken.text, right)
        }

        return left
    }

    private fun parseAtom(stream: TokenStream): Expression {
        if (!stream.hasNext()) {
            error("Unexpected end of expression, expected a value")
        }

        val token = stream.peek()

        val prefixOp = findPrefixOperator(token)
        if (prefixOp != null) {
            stream.advance()
            val operand = parseExpression(stream, prefixOp.precedence)
            return UnaryExpression(token.text, operand)
        }

        if (groupOpen != null && groupOpen.match(token.text)) {
            stream.advance()
            val expr = parseExpression(stream, 0)
            if (!stream.hasNext() || groupClose == null || !groupClose.match(stream.peek().text)) {
                error("Expected closing '${groupClose?.symbols?.first() ?: ")"}' after grouped expression")
            }
            stream.advance()
            return expr
        }

        stream.advance()
        return build(token)
    }

    private fun findOperator(token: Token): OperatorDef? =
        operators.firstOrNull { it.definition.match(token.text) }

    private fun findPrefixOperator(token: Token): PrefixOperatorDef? =
        prefixOperators.firstOrNull { it.definition.match(token.text) }

    private class TokenStream(private val tokens: List<Token>) {
        private var pos = 0
        fun hasNext(): Boolean = pos < tokens.size
        fun peek(): Token = tokens[pos]
        fun advance(): Token = tokens[pos++]
    }
}
