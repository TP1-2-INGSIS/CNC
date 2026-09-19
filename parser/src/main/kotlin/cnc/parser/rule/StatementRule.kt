package cnc.parser.rule

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
    fun match(def: TokenDefinition, type: TokenType? = null): Boolean
    fun expect(type: TokenType, text: String? = null): Token
    fun expect(def: TokenDefinition, type: TokenType? = null): Token
    fun advance(): Token
    fun parseExpression(): Expression
    fun parseStatement(): Statement
    fun parseBlock(): cnc.ast.BlockStatement
}

/**
 * Contract for a predictive statement parsing rule.
 */
interface StatementRule<T : Statement> {
    val tag: String

    fun canStart(cursor: Cursor<Token>): Boolean
    fun parse(cursor: Cursor<Token>, expressionParser: ExpressionParser, nextStatement: (Cursor<Token>) -> Result<Statement>): Result<T>

    companion object {
        fun <T : Statement> builder(tag: String): StatementRuleBuilder<T> = StatementRuleBuilder(tag)
    }
}

/**
 * Fluent builder for creating predictive [StatementRule]s without scattered boilerplate.
 */
class StatementRuleBuilder<T : Statement>(val tag: String) {
    private var canStartPredicate: ((Cursor<Token>) -> Boolean)? = null
    private var parseBlock: (ParseContext.() -> T)? = null

    fun canStart(predicate: (Cursor<Token>) -> Boolean): StatementRuleBuilder<T> = apply {
        this.canStartPredicate = predicate
    }

    fun canStartWith(tokenDef: TokenDefinition, type: TokenType? = null): StatementRuleBuilder<T> = apply {
        this.canStartPredicate = { cursor ->
            val first = cursor.peek(0)
            first != null && (type == null || first.type == type) && tokenDef.match(first.text)
        }
    }

    fun canStartWith(vararg tokenDefs: TokenDefinition, type: TokenType? = null): StatementRuleBuilder<T> = apply {
        this.canStartPredicate = { cursor ->
            val first = cursor.peek(0)
            first != null && (type == null || first.type == type) && tokenDefs.any { it.match(first.text) }
        }
    }

    fun parse(block: ParseContext.() -> T): StatementRuleBuilder<T> = apply {
        this.parseBlock = block
    }

    fun build(): StatementRule<T> {
        val canStart = checkNotNull(canStartPredicate) { "canStart predicate must be defined for rule '$tag'" }
        val parse = checkNotNull(parseBlock) { "parse block must be defined for rule '$tag'" }
        return statementRule(tag, canStart, parse)
    }
}

/**
 * Builder for creating strongly typed, predictive statement rules with functional [Result] handling.
 */
fun <T : Statement> statementRule(
    tag: String,
    canStart: (Cursor<Token>) -> Boolean,
    action: ParseContext.() -> T
): StatementRule<T> = object : StatementRule<T> {
    override val tag: String = tag

    override fun canStart(cursor: Cursor<Token>): Boolean = canStart(cursor)

    override fun parse(cursor: Cursor<Token>, expressionParser: ExpressionParser, nextStatement: (Cursor<Token>) -> Result<Statement>): Result<T> {
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

            override fun match(def: TokenDefinition, type: TokenType?): Boolean {
                val next = cursor.peek() ?: return false
                val matchesType = type == null || next.type == type
                val matchesDef = def.match(next.text)
                return if (matchesType && matchesDef) {
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

            override fun expect(def: TokenDefinition, type: TokenType?): Token {
                val next = cursor.peek()
                val expected = def.symbols.firstOrNull() ?: def.alias
                if (next == null) {
                    throw ParseAbortException(
                        Failure<Nothing>("Syntax error: unexpected end of file, expected '$expected'", ErrorType.PARSER)
                    )
                }
                val matchesType = type == null || next.type == type
                val matchesDef = def.match(next.text)
                if (!matchesType || !matchesDef) {
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

            override fun parseStatement(): Statement {
                val res = nextStatement(cursor)
                if (res is Failure) {
                    throw ParseAbortException(Failure<Nothing>(res.msg, res.type))
                }
                return (res as Success).data
            }

            override fun parseBlock(): cnc.ast.BlockStatement {
                expect(CncSymbols.OPEN_BRACE)
                val stmts = mutableListOf<Statement>()
                while (peek()?.let { CncSymbols.CLOSE_BRACE.match(it.text) } != true) {
                    stmts.add(parseStatement())
                }
                expect(CncSymbols.CLOSE_BRACE)
                return cnc.ast.BlockStatement(stmts)
            }
        }

        return try {
            val statement = context.action()
            Success("Parsed $tag", statement)
        } catch (e: ParseAbortException) {
            Failure(e.failure.msg, e.failure.type)
        }
    }
}
