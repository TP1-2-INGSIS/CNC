package cnc.ast

import ast.StatementVisitor
import ast.ExpressionVisitor
import cnc.token.Token
import cnc.token.TokenDefinition

// TODO: Crear evaluators para cada statement
// el Parser puede crear los AST conociendolos
// y como comparte con el Interpreter no hay problema 
// mientras tengamos los Evaluators de cada statement.

sealed interface Statement {
    fun <R> accept(visitor: StatementVisitor<R>): R;
}
data class Declaration(
    val name: String,
    val type: String,
    val value: Expression?
) : Statement { override fun <R> accept(visitor: StatementVisitor<R>) = visitor.visit(this); }
data class Assignment(
    val target: String,
    val value: Expression
) : Statement {override fun <R> accept(visitor: StatementVisitor<R>) = visitor.visit(this);}
data class Call(
    val function: String,
    val arguments: List<Expression>
) : Statement {override fun <R> accept(visitor: StatementVisitor<R>) = visitor.visit(this);}

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
) : Expression
data class UnaryExpression(
    val operator: String,
    val operand: Expression
) : Expression

/**
 * Asociatividad de un operador binario.
 */
enum class Associativity { LEFT, RIGHT }

/**
 * Definición de un operador binario: su token definition, precedencia y asociatividad.
 */
data class OperatorDef(
    val definition: TokenDefinition,
    val precedence: Int,
    val associativity: Associativity = Associativity.LEFT
)

/**
 * Definición de un operador unario (prefix): su token definition y precedencia.
 */
data class PrefixOperatorDef(
    val definition: TokenDefinition,
    val precedence: Int
)

/**
 * Construye expresiones a partir de una lista de tokens usando un Pratt parser.
 *
 * @param recipes         mapa de TokenDefinition → builder para átomos (literales, identificadores)
 * @param operators       lista de operadores binarios con precedencia y asociatividad
 * @param prefixOperators lista de operadores unarios prefix (ej: negación)
 * @param groupOpen       token definition para paréntesis de apertura (opcional)
 * @param groupClose      token definition para paréntesis de cierre (opcional)
 */
class ExpressionBuilder(
    private val recipes: Map<TokenDefinition, (Token) -> Expression>,
    private val operators: List<OperatorDef> = emptyList(),
    private val prefixOperators: List<PrefixOperatorDef> = emptyList(),
    private val groupOpen: TokenDefinition? = null,
    private val groupClose: TokenDefinition? = null
) {

    /**
     * Construye una expresión a partir de un solo token (átomo).
     */
    fun build(token: Token): Expression {
        val (_, builder) = recipes.entries.firstOrNull { (definition, _) ->
            definition.match(token.text)
        } ?: error("No recipe matches token: '${token.text}'")
        return builder(token)
    }

    /**
     * Construye una expresión a partir de una lista de tokens,
     * respetando precedencia y asociatividad de operadores.
     */
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

    // -------------------------------------------------------------------------
    // Pratt Parser internals
    // -------------------------------------------------------------------------

    private fun parseExpression(stream: TokenStream, minPrecedence: Int): Expression {
        var left = parseAtom(stream)

        while (stream.hasNext()) {
            val opToken = stream.peek()
            val opDef = findOperator(opToken) ?: break

            if (opDef.precedence < minPrecedence) break

            stream.advance() // consumir el operador

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

        // Manejo de operadores unarios prefix: -expr, !expr
        val prefixOp = findPrefixOperator(token)
        if (prefixOp != null) {
            stream.advance() // consumir el operador prefix
            val operand = parseExpression(stream, prefixOp.precedence)
            return UnaryExpression(token.text, operand)
        }

        // Manejo de paréntesis: ( expr )
        if (groupOpen != null && groupOpen.match(token.text)) {
            stream.advance() // consumir '('
            val expr = parseExpression(stream, 0)
            if (!stream.hasNext() || groupClose == null || !groupClose.match(stream.peek().text)) {
                error("Expected closing '${groupClose?.symbols?.first() ?: ")"}' after grouped expression")
            }
            stream.advance() // consumir ')'
            return expr
        }

        stream.advance()
        return build(token)
    }

    private fun findOperator(token: Token): OperatorDef? {
        return operators.firstOrNull { it.definition.match(token.text) }
    }

    private fun findPrefixOperator(token: Token): PrefixOperatorDef? {
        return prefixOperators.firstOrNull { it.definition.match(token.text) }
    }

    // -------------------------------------------------------------------------
    // Simple token stream for the Pratt parser
    // -------------------------------------------------------------------------

    private class TokenStream(private val tokens: List<Token>) {
        private var pos = 0

        fun hasNext(): Boolean = pos < tokens.size
        fun peek(): Token = tokens[pos]
        fun advance(): Token = tokens[pos++]
    }
}
