package cnc.parser.flow

import cnc.ast.BlockStatement
import cnc.ast.Expression
import cnc.ast.Statement
import cnc.common.Cursor
import cnc.common.ErrorType
import cnc.common.Failure
import cnc.common.Result
import cnc.common.Success
import cnc.parser.expression.ExpressionParser
import cnc.token.CncSymbols
import cnc.token.Token
import cnc.token.TokenDefinition
import cnc.token.TokenType

/**
 * Monadic, railroad-oriented flow for sequentially parsing and validating tokens from a [Cursor].
 * Transitions to a failed state on the first mismatch, rendering subsequent operations no-ops
 * and cleanly terminating in a [Result] via [.then] or [.catch] without throwing exceptions.
 */
sealed interface ParseFlow {
    val cursor: Cursor<Token>

    fun expect(def: TokenDefinition): ParseFlow
    fun expect(type: TokenType, text: String? = null): ParseFlow

    fun capture(def: TokenDefinition, action: (Token) -> Unit): ParseFlow
    fun capture(type: TokenType, action: (Token) -> Unit): ParseFlow
    fun capture(type: TokenType, text: String?, action: (Token) -> Unit): ParseFlow

    fun optional(def: TokenDefinition, action: (Token) -> Unit = {}): ParseFlow
    fun optional(type: TokenType, action: (Token) -> Unit = {}): ParseFlow

    fun parseExpression(expressionParser: ExpressionParser, action: (Expression) -> Unit): ParseFlow

    fun parseBlock(
        openBrace: TokenDefinition = CncSymbols.OPEN_BRACE,
        closeBrace: TokenDefinition = CncSymbols.CLOSE_BRACE,
        statementParser: (Cursor<Token>) -> Result<Statement>,
        action: (BlockStatement) -> Unit
    ): ParseFlow

    fun <T> then(transform: () -> T): Result<T>
    fun <T> then(tag: String, transform: () -> T): Result<T>

    fun catch(handler: (Failure<Nothing>) -> Failure<Nothing>): ParseFlow
}

class OkFlow(override val cursor: Cursor<Token>) : ParseFlow {

    override fun expect(def: TokenDefinition): ParseFlow {
        val next = cursor.peek()
        val expected = def.symbols.firstOrNull() ?: def.alias
        if (next == null) {
            return FailedFlow(
                cursor,
                Failure("Syntax error: unexpected end of file, expected '$expected'", ErrorType.PARSER)
            )
        }
        if (!def.match(next.text)) {
            return FailedFlow(
                cursor,
                Failure(
                    "Syntax error at row ${next.pos.row}, col ${next.pos.col}: expected '$expected' but found '${next.text}'",
                    ErrorType.PARSER
                )
            )
        }
        cursor.advance()
        return this
    }

    override fun expect(type: TokenType, text: String?): ParseFlow {
        val next = cursor.peek()
        val expected = text ?: type.name
        if (next == null) {
            return FailedFlow(
                cursor,
                Failure("Syntax error: unexpected end of file, expected '$expected'", ErrorType.PARSER)
            )
        }
        val matchesType = next.type == type
        val matchesText = text == null || next.text == text
        if (!matchesType || !matchesText) {
            return FailedFlow(
                cursor,
                Failure(
                    "Syntax error at row ${next.pos.row}, col ${next.pos.col}: expected '$expected' but found '${next.text}'",
                    ErrorType.PARSER
                )
            )
        }
        cursor.advance()
        return this
    }

    override fun capture(def: TokenDefinition, action: (Token) -> Unit): ParseFlow {
        val next = cursor.peek()
        val expected = def.symbols.firstOrNull() ?: def.alias
        if (next == null) {
            return FailedFlow(
                cursor,
                Failure("Syntax error: unexpected end of file, expected '$expected'", ErrorType.PARSER)
            )
        }
        if (!def.match(next.text)) {
            return FailedFlow(
                cursor,
                Failure(
                    "Syntax error at row ${next.pos.row}, col ${next.pos.col}: expected '$expected' but found '${next.text}'",
                    ErrorType.PARSER
                )
            )
        }
        val token = cursor.advance()!!
        action(token)
        return this
    }

    override fun capture(type: TokenType, text: String?, action: (Token) -> Unit): ParseFlow {
        val next = cursor.peek()
        val expected = text ?: type.name
        if (next == null) {
            return FailedFlow(
                cursor,
                Failure("Syntax error: unexpected end of file, expected '$expected'", ErrorType.PARSER)
            )
        }
        val matchesType = next.type == type
        val matchesText = text == null || next.text == text
        if (!matchesType || !matchesText) {
            return FailedFlow(
                cursor,
                Failure(
                    "Syntax error at row ${next.pos.row}, col ${next.pos.col}: expected '$expected' but found '${next.text}'",
                    ErrorType.PARSER
                )
            )
        }
        val token = cursor.advance()!!
        action(token)
        return this
    }

    override fun capture(type: TokenType, action: (Token) -> Unit): ParseFlow =
        capture(type, null, action)

    override fun optional(def: TokenDefinition, action: (Token) -> Unit): ParseFlow {
        val next = cursor.peek()
        if (next != null && def.match(next.text)) {
            val token = cursor.advance()!!
            action(token)
        }
        return this
    }

    override fun optional(type: TokenType, action: (Token) -> Unit): ParseFlow {
        val next = cursor.peek()
        if (next != null && next.type == type) {
            val token = cursor.advance()!!
            action(token)
        }
        return this
    }

    override fun parseExpression(expressionParser: ExpressionParser, action: (Expression) -> Unit): ParseFlow {
        return try {
            val expr = expressionParser.parse(cursor)
            action(expr)
            this
        } catch (e: Exception) {
            val current = cursor.peek()
            val posStr = if (current?.pos != null) " at row ${current.pos.row}, col ${current.pos.col}" else ""
            FailedFlow(
                cursor,
                Failure("Syntax error in expression$posStr: ${e.message}", ErrorType.PARSER)
            )
        }
    }

    override fun parseBlock(
        openBrace: TokenDefinition,
        closeBrace: TokenDefinition,
        statementParser: (Cursor<Token>) -> Result<Statement>,
        action: (BlockStatement) -> Unit
    ): ParseFlow {
        val next = cursor.peek()
        val expectedOpen = openBrace.symbols.firstOrNull() ?: openBrace.alias
        if (next == null) {
            return FailedFlow(
                cursor,
                Failure("Syntax error: unexpected end of file, expected '$expectedOpen'", ErrorType.PARSER)
            )
        }
        if (!openBrace.match(next.text)) {
            return FailedFlow(
                cursor,
                Failure(
                    "Syntax error at row ${next.pos.row}, col ${next.pos.col}: expected '$expectedOpen' but found '${next.text}'",
                    ErrorType.PARSER
                )
            )
        }
        cursor.advance()

        val statements = mutableListOf<Statement>()
        while (cursor.hasMore()) {
            val peeked = cursor.peek() ?: break
            if (closeBrace.match(peeked.text)) {
                break
            }
            when (val res = statementParser(cursor)) {
                is Success -> statements.add(res.data)
                is Failure -> return FailedFlow(cursor, Failure(res.msg, res.type))
            }
        }

        val closing = cursor.peek()
        val expectedClose = closeBrace.symbols.firstOrNull() ?: closeBrace.alias
        if (closing == null) {
            return FailedFlow(
                cursor,
                Failure("Syntax error: unexpected end of file, expected '$expectedClose'", ErrorType.PARSER)
            )
        }
        if (!closeBrace.match(closing.text)) {
            return FailedFlow(
                cursor,
                Failure(
                    "Syntax error at row ${closing.pos.row}, col ${closing.pos.col}: expected '$expectedClose' but found '${closing.text}'",
                    ErrorType.PARSER
                )
            )
        }
        cursor.advance()

        action(BlockStatement(statements))
        return this
    }

    override fun <T> then(transform: () -> T): Result<T> =
        Success("ok", transform())

    override fun <T> then(tag: String, transform: () -> T): Result<T> =
        Success(tag, transform())

    override fun catch(handler: (Failure<Nothing>) -> Failure<Nothing>): ParseFlow = this
}

class FailedFlow(
    override val cursor: Cursor<Token>,
    val failure: Failure<Nothing>
) : ParseFlow {

    override fun expect(def: TokenDefinition): ParseFlow = this
    override fun expect(type: TokenType, text: String?): ParseFlow = this

    override fun capture(def: TokenDefinition, action: (Token) -> Unit): ParseFlow = this
    override fun capture(type: TokenType, action: (Token) -> Unit): ParseFlow = this
    override fun capture(type: TokenType, text: String?, action: (Token) -> Unit): ParseFlow = this

    override fun optional(def: TokenDefinition, action: (Token) -> Unit): ParseFlow = this
    override fun optional(type: TokenType, action: (Token) -> Unit): ParseFlow = this

    override fun parseExpression(expressionParser: ExpressionParser, action: (Expression) -> Unit): ParseFlow = this

    override fun parseBlock(
        openBrace: TokenDefinition,
        closeBrace: TokenDefinition,
        statementParser: (Cursor<Token>) -> Result<Statement>,
        action: (BlockStatement) -> Unit
    ): ParseFlow = this

    override fun <T> then(transform: () -> T): Result<T> =
        Failure(failure.msg, failure.type)

    override fun <T> then(tag: String, transform: () -> T): Result<T> =
        Failure(failure.msg, failure.type)

    override fun catch(handler: (Failure<Nothing>) -> Failure<Nothing>): ParseFlow =
        FailedFlow(cursor, handler(failure))
}
