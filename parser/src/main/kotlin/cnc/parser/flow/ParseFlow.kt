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

    /**
     * Executes a sub-parser [operation] on the cursor.
     * If successful, passes the produced value to [action] and continues the flow.
     * If failed, transitions the flow to [FailedFlow].
     */
    fun <R> step(operation: (Cursor<Token>) -> Result<R>, action: (R) -> Unit): ParseFlow

    /**
     * If the next token matches [trigger], consumes it and executes [operation].
     * Transitions to [FailedFlow] if [operation] fails.
     */
    fun <R> optionalStep(trigger: TokenDefinition, operation: (Cursor<Token>) -> Result<R>, action: (R) -> Unit): ParseFlow
    fun <R> optionalStep(triggerType: TokenType, triggerText: String? = null, operation: (Cursor<Token>) -> Result<R>, action: (R) -> Unit): ParseFlow

    fun parseExpression(expressionParser: ExpressionParser, action: (Expression) -> Unit): ParseFlow
    fun optionalExpression(trigger: TokenDefinition, expressionParser: ExpressionParser, action: (Expression) -> Unit): ParseFlow

    fun parseBlock(
        statementParser: (Cursor<Token>) -> Result<Statement>,
        action: (BlockStatement) -> Unit
    ): ParseFlow

    fun <T> then(transform: () -> T): Result<T>
    fun <T> then(tag: String, transform: () -> T): Result<T>
    fun <T> thenResult(transform: () -> Result<T>): Result<T>

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

    override fun <R> step(operation: (Cursor<Token>) -> Result<R>, action: (R) -> Unit): ParseFlow {
        return when (val res = operation(cursor)) {
            is Success -> {
                action(res.data)
                this
            }
            is Failure -> FailedFlow(cursor, Failure(res.msg, res.type))
        }
    }

    override fun <R> optionalStep(
        trigger: TokenDefinition,
        operation: (Cursor<Token>) -> Result<R>,
        action: (R) -> Unit
    ): ParseFlow {
        val next = cursor.peek()
        if (next != null && trigger.match(next.text)) {
            cursor.advance()
            return when (val res = operation(cursor)) {
                is Success -> {
                    action(res.data)
                    this
                }
                is Failure -> FailedFlow(cursor, Failure(res.msg, res.type))
            }
        }
        return this
    }

    override fun <R> optionalStep(
        triggerType: TokenType,
        triggerText: String?,
        operation: (Cursor<Token>) -> Result<R>,
        action: (R) -> Unit
    ): ParseFlow {
        val next = cursor.peek()
        val matchesType = next?.type == triggerType
        val matchesText = triggerText == null || next?.text == triggerText
        if (matchesType && matchesText) {
            cursor.advance()
            return when (val res = operation(cursor)) {
                is Success -> {
                    action(res.data)
                    this
                }
                is Failure -> FailedFlow(cursor, Failure(res.msg, res.type))
            }
        }
        return this
    }

    override fun parseExpression(expressionParser: ExpressionParser, action: (Expression) -> Unit): ParseFlow =
        step(expressionParser::parse, action)

    override fun optionalExpression(
        trigger: TokenDefinition,
        expressionParser: ExpressionParser,
        action: (Expression) -> Unit
    ): ParseFlow = optionalStep(trigger, expressionParser::parse, action)

    override fun parseBlock(
        statementParser: (Cursor<Token>) -> Result<Statement>,
        action: (BlockStatement) -> Unit
    ): ParseFlow {
        val openFlow = expect(CncSymbols.OPEN_BRACE)
        if (openFlow is FailedFlow) return openFlow

        val statements = mutableListOf<Statement>()
        while (cursor.hasMore() && cursor.peek()?.let { CncSymbols.CLOSE_BRACE.match(it.text) } != true) {
            when (val res = statementParser(cursor)) {
                is Success -> statements.add(res.data)
                is Failure -> return FailedFlow(cursor, Failure(res.msg, res.type))
            }
        }

        val closeFlow = expect(CncSymbols.CLOSE_BRACE)
        if (closeFlow is FailedFlow) return closeFlow

        action(BlockStatement(statements))
        return this
    }

    override fun <T> then(transform: () -> T): Result<T> =
        Success("ok", transform())

    override fun <T> then(tag: String, transform: () -> T): Result<T> =
        Success(tag, transform())

    override fun <T> thenResult(transform: () -> Result<T>): Result<T> =
        transform()

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

    override fun <R> step(operation: (Cursor<Token>) -> Result<R>, action: (R) -> Unit): ParseFlow = this
    override fun <R> optionalStep(trigger: TokenDefinition, operation: (Cursor<Token>) -> Result<R>, action: (R) -> Unit): ParseFlow = this
    override fun <R> optionalStep(triggerType: TokenType, triggerText: String?, operation: (Cursor<Token>) -> Result<R>, action: (R) -> Unit): ParseFlow = this

    override fun parseExpression(expressionParser: ExpressionParser, action: (Expression) -> Unit): ParseFlow = this
    override fun optionalExpression(trigger: TokenDefinition, expressionParser: ExpressionParser, action: (Expression) -> Unit): ParseFlow = this
    override fun parseBlock(
        statementParser: (Cursor<Token>) -> Result<Statement>,
        action: (BlockStatement) -> Unit
    ): ParseFlow = this

    override fun <T> then(transform: () -> T): Result<T> =
        Failure(failure.msg, failure.type)

    override fun <T> then(tag: String, transform: () -> T): Result<T> =
        Failure(failure.msg, failure.type)

    override fun <T> thenResult(transform: () -> Result<T>): Result<T> =
        Failure(failure.msg, failure.type)

    override fun catch(handler: (Failure<Nothing>) -> Failure<Nothing>): ParseFlow =
        FailedFlow(cursor, handler(failure))
}
