package cnc.parser.expression

import cnc.ast.BinaryExpression
import cnc.ast.Expression
import cnc.ast.UnaryExpression
import cnc.ast.CallExpression
import cnc.ast.Identifier
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
        } ?: throw cnc.parser.rule.ParseAbortException(
            cnc.common.Failure("No recipe matches token: '${token.text}' at row ${token.pos.row}, col ${token.pos.col}", cnc.common.ErrorType.PARSER)
        )
        return builder(token)
    }

    fun build(tokens: List<Token>): Expression {
        if (tokens.isEmpty()) throw cnc.parser.rule.ParseAbortException(
            cnc.common.Failure("Cannot build expression from empty token list", cnc.common.ErrorType.PARSER)
        )
        if (operators.isEmpty() || tokens.size == 1) {
            return build(tokens.first())
        }
        val cursor = tokens.asCursor()
        val result = parse(cursor)
        if (cursor.hasMore()) {
            throw cnc.parser.rule.ParseAbortException(
                cnc.common.Failure("Unexpected token '${cursor.peek()?.text}' after expression", cnc.common.ErrorType.PARSER)
            )
        }
        return result
    }

    override fun parse(cursor: Cursor<Token>): Expression = parseExpression(cursor, 0)

    private fun parseExpression(cursor: Cursor<Token>, minPrecedence: Int): Expression {
        var left = parseAtom(cursor)

        while (cursor.hasMore()) {
            val token = cursor.peek() ?: break
            val operator = findOperator(token) ?: break

            if (operator.precedence < minPrecedence) break

            cursor.advance()

            val nextMinPrecedence = if (operator.associativity == Associativity.LEFT) {
                operator.precedence + 1
            } else {
                operator.precedence
            }

            val right = parseExpression(cursor, nextMinPrecedence)
            left = BinaryExpression(left, token.text, right)
        }

        return left
    }

    private fun parseAtom(cursor: Cursor<Token>): Expression {
        if (!cursor.hasMore()) {
            throw cnc.parser.rule.ParseAbortException(
                cnc.common.Failure("Unexpected end of expression, expected a value", cnc.common.ErrorType.PARSER)
            )
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
                throw cnc.parser.rule.ParseAbortException(
                    cnc.common.Failure("Expected closing '${groupClose?.symbols?.first() ?: ")"}' after grouped expression", cnc.common.ErrorType.PARSER)
                )
            }
            cursor.advance()
            return expr
        }

        cursor.advance()
        val atom = build(token)
        
        val nextToken = cursor.peek()
        if (atom is Identifier && nextToken != null && nextToken.text == "(") {
            cursor.advance() // consume "("
            val args = mutableListOf<Expression>()
            if (cursor.peek()?.text != ")") {
                args.add(parseExpression(cursor, 0))
                while (cursor.peek()?.text == ",") {
                    cursor.advance() // consume ","
                    args.add(parseExpression(cursor, 0))
                }
            }
            val closeToken = cursor.peek()
            if (closeToken == null || closeToken.text != ")") {
                throw cnc.parser.rule.ParseAbortException(
                    cnc.common.Failure("Expected closing ')' after arguments", cnc.common.ErrorType.PARSER)
                )
            }
            cursor.advance() // consume ")"
            return CallExpression(atom.name, args)
        }
        
        return atom
    }

    private fun findOperator(token: Token): OperatorDef? =
        operators.firstOrNull { it.definition.match(token.text) }

    private fun findPrefixOperator(token: Token): PrefixOperatorDef? =
        prefixOperators.firstOrNull { it.definition.match(token.text) }
}
