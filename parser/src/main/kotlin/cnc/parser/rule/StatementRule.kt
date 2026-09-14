package cnc.parser.rule

import cnc.ast.Expression
import cnc.ast.Statement
import cnc.common.Cursor
import cnc.common.ErrorType
import cnc.common.Failure
import cnc.common.Result
import cnc.common.Success
import cnc.parser.expression.ExpressionParser
import cnc.token.Token
import cnc.token.TokenType

/**
 * Exception used internally to short-circuit rule execution on parse error.
 * Caught by [statementRule] and converted to [Failure].
 */
class ParseAbortException(val failure: Failure<Nothing>) : RuntimeException(failure.msg)

/**
 * Context provided inside a statement rule's parse block.
 */
interface ParseContext {
    val cursor: Cursor<Token>

    fun peek(offset: Int = 0): Token?
    fun match(type: TokenType, text: String? = null): Boolean
    fun expect(type: TokenType, text: String? = null): Token
    fun advance(): Token
    fun parseExpression(): Expression
}

/**
 * Contract for a predictive statement parsing rule.
 */
interface StatementRule<T : Statement> {
    val tag: String

    fun canStart(cursor: Cursor<Token>): Boolean
    fun parse(cursor: Cursor<Token>, expressionParser: ExpressionParser): Result<T>
}

/**
 * Builder for creating strongly typed, predictive statement rules with functional [Result] handling.
 */
fun <T : Statement> statementRule(
    tag: String,
    canStart: (Cursor<Token>) -> Boolean,
    parseBlock: ParseContext.() -> T
): StatementRule<T> = object : StatementRule<T> {
    override val tag: String = tag

    override fun canStart(cursor: Cursor<Token>): Boolean = canStart(cursor)

    override fun parse(cursor: Cursor<Token>, expressionParser: ExpressionParser): Result<T> {
        val context = object : ParseContext {
            override val cursor: Cursor<Token> = cursor

            override fun peek(offset: Int): Token? = cursor.peek(offset)

            override fun match(type: TokenType, text: String?): Boolean {
                val next = cursor.peek() ?: return false
                val matchesType = next.type == type
                val matchesText = text == null || next.text == text
                return if (matchesType && matchesText) {
                    cursor.advance()
                    true
                } else false
            }

            override fun expect(type: TokenType, text: String?): Token {
                val next = cursor.peek()
                if (next == null) {
                    val expected = text ?: type.name
                    throw ParseAbortException(
                        Failure<Nothing>("Syntax error: unexpected end of file, expected '$expected'", ErrorType.PARSER)
                    )
                }
                val matchesType = next.type == type
                val matchesText = text == null || next.text == text
                if (!matchesType || !matchesText) {
                    val expected = text ?: type.name
                    throw ParseAbortException(
                        Failure<Nothing>("Syntax error at row ${next.pos.row}, col ${next.pos.col}: expected '$expected' but found '${next.text}'", ErrorType.PARSER)
                    )
                }
                return cursor.advance()!!
            }

            override fun advance(): Token {
                return cursor.advance() ?: throw ParseAbortException(
                    Failure<Nothing>("Syntax error: unexpected end of file", ErrorType.PARSER)
                )
            }

            override fun parseExpression(): Expression {
                return try {
                    expressionParser.parse(cursor)
                } catch (e: Exception) {
                    val current = cursor.peek()
                    val pos = current?.pos
                    val posStr = if (pos != null) " at row ${pos.row}, col ${pos.col}" else ""
                    throw ParseAbortException(
                        Failure<Nothing>("Syntax error in expression$posStr: ${e.message}", ErrorType.PARSER)
                    )
                }
            }
        }

        return try {
            val statement = context.parseBlock()
            Success("Parsed $tag", statement)
        } catch (e: ParseAbortException) {
            Failure(e.failure.msg, e.failure.type)
        }
    }
}
