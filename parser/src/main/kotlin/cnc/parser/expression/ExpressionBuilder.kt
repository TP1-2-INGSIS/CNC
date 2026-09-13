package cnc.parser.expression

import cnc.ast.BinaryExpression
import cnc.ast.Expression
import cnc.ast.UnaryExpression
import cnc.common.Cursor
import cnc.common.asCursor
import cnc.token.Token
import cnc.token.TokenDefinition

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

interface ExpressionParser {
    fun parse(cursor: Cursor<Token>): Expression
}

/**
 * Pratt parser implementation for expressions with precedence and associativity.
 */
class ExpressionBuilder(
    private val recipes: Map<TokenDefinition, (Token) -> Expression>,
    private val operators: List<OperatorDef> = emptyList(),
    private val prefixOperators: List<PrefixOperatorDef> = emptyList(),
    private val groupOpen: TokenDefinition? = null,
    private val groupClose: TokenDefinition? = null
) : ExpressionParser {

    fun build(token: Token): Expression {
        val (_, builder) = recipes.entries.firstOrNull { (definition, _) ->
            definition.match(token.text)
        } ?: error("No recipe matches token: '${token.text}' at row ${token.pos.row}, col ${token.pos.col}")
        return builder(token)
    }

    fun build(tokens: List<Token>): Expression {
        if (tokens.isEmpty()) error("Cannot build expression from empty token list")
        if (operators.isEmpty() || tokens.size == 1) {
            return build(tokens.first())
        }
        val cursor = tokens.asCursor()
        val result = parse(cursor)
        if (cursor.hasMore()) {
            error("Unexpected token '${cursor.peek()?.text}' after expression")
        }
        return result
    }

    override fun parse(cursor: Cursor<Token>): Expression = parseExpression(cursor, 0)

    private fun parseExpression(cursor: Cursor<Token>, minPrecedence: Int): Expression {
        var left = parseAtom(cursor)

        while (cursor.hasMore()) {
            val opToken = cursor.peek() ?: break
            val opDef = findOperator(opToken) ?: break
            if (opDef.precedence < minPrecedence) break

            cursor.advance()
            val nextMinPrecedence = when (opDef.associativity) {
                Associativity.LEFT -> opDef.precedence + 1
                Associativity.RIGHT -> opDef.precedence
            }
            val right = parseExpression(cursor, nextMinPrecedence)
            left = BinaryExpression(left, opToken.text, right)
        }

        return left
    }

    private fun parseAtom(cursor: Cursor<Token>): Expression {
        if (!cursor.hasMore()) {
            error("Unexpected end of expression, expected a value")
        }

        val token = cursor.peek()!!

        val prefixOp = findPrefixOperator(token)
        if (prefixOp != null) {
            cursor.advance()
            val operand = parseExpression(cursor, prefixOp.precedence)
            return UnaryExpression(token.text, operand)
        }

        if (groupOpen != null && groupOpen.match(token.text)) {
            cursor.advance()
            val expr = parseExpression(cursor, 0)
            val closing = cursor.peek()
            if (closing == null || groupClose == null || !groupClose.match(closing.text)) {
                error("Expected closing '${groupClose?.symbols?.first() ?: ")"}' after grouped expression")
            }
            cursor.advance()
            return expr
        }

        cursor.advance()
        return build(token)
    }

    private fun findOperator(token: Token): OperatorDef? =
        operators.firstOrNull { it.definition.match(token.text) }

    private fun findPrefixOperator(token: Token): PrefixOperatorDef? =
        prefixOperators.firstOrNull { it.definition.match(token.text) }
}
