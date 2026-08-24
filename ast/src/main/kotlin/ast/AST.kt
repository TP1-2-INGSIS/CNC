package cnc.ast

import cnc.token.Token
import cnc.token.TokenDefinition

// TODO: Crear evaluators para cada statement
// el Parser puede crear los AST conociendolos
// y como comparte con el Interpreter no hay problema 
// mientras tengamos los Evaluators de cada statement.

sealed interface Statement
data class Declaration(
    val name: String,
    val type: String,
    val value: Expression?
) : Statement
data class Assignment(
    val target: String,
    val value: Expression
) : Statement
data class Call(
    val function: String,
    val arguments: List<Expression>
) : Statement

sealed interface Expression
data class NumberLiteral(val value: Double) : Expression
data class StringLiteral(val value: String) : Expression
data class Identifier(val name: String) : Expression
data class BinaryExpression(
    val left: Expression,
    val operator: String,
    val right: Expression
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
 * Construye expresiones a partir de una lista de tokens usando un Pratt parser.
 *
 * @param recipes    mapa de TokenDefinition → builder para átomos (literales, identificadores)
 * @param operators  lista de operadores binarios con precedencia y asociatividad
 */
class ExpressionBuilder(
    private val recipes: Map<TokenDefinition, (Token) -> Expression>,
    private val operators: List<OperatorDef> = emptyList()
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
        return parseExpression(stream, 0)
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
        val token = stream.advance()
        return build(token)
    }

    private fun findOperator(token: Token): OperatorDef? {
        return operators.firstOrNull { it.definition.match(token.text) }
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
