package cnc.parser.rule

import cnc.ast.Statement
import cnc.common.Cursor
import cnc.common.ErrorType
import cnc.common.Failure
import cnc.common.Result
import cnc.parser.expression.ExpressionParser
import cnc.token.Token

/**
 * Exception retained for backwards compatibility with ExpressionBuilder until Phase 5.
 */
class ParseAbortException(val failure: Failure<Nothing>) : RuntimeException(failure.msg)

/**
 * Contract for a predictive statement parsing rule.
 */
interface StatementRule<out T : Statement> {
    val tag: String

    fun canStart(cursor: Cursor<Token>): Boolean
    fun parse(
        cursor: Cursor<Token>,
        expressionParser: ExpressionParser,
        nextStatement: (Cursor<Token>) -> Result<Statement> = {
            Failure("No statement parser provided", ErrorType.PARSER)
        }
    ): Result<T>

    companion object {
        fun <T : Statement> builder(): StatementRuleBuilder<T> = StatementRuleBuilder()
        fun <T : Statement> builder(tag: String): StatementRuleBuilder<T> = StatementRuleBuilder<T>().withName(tag)
    }
}

/**
 * Strongly typed, fluent builder for predictive [StatementRule]s.
 */
class StatementRuleBuilder<T : Statement> {
    private var name: String = ""
    private var startsPredicate: ((Cursor<Token>) -> Boolean)? = null
    private var parseBlock: ((cursor: Cursor<Token>, expressionParser: ExpressionParser, nextStatement: (Cursor<Token>) -> Result<Statement>) -> Result<T>)? = null

    fun withName(name: String): StatementRuleBuilder<T> = apply {
        this.name = name
    }

    fun starts(predicate: (Cursor<Token>) -> Boolean): StatementRuleBuilder<T> = apply {
        this.startsPredicate = predicate
    }

    fun canStart(predicate: (Cursor<Token>) -> Boolean): StatementRuleBuilder<T> = starts(predicate)

    fun parse(block: (cursor: Cursor<Token>, expressionParser: ExpressionParser) -> Result<T>): StatementRuleBuilder<T> = apply {
        this.parseBlock = { cursor, expressionParser, _ -> block(cursor, expressionParser) }
    }

    fun parseWithStatements(
        block: (cursor: Cursor<Token>, expressionParser: ExpressionParser, nextStatement: (Cursor<Token>) -> Result<Statement>) -> Result<T>
    ): StatementRuleBuilder<T> = apply {
        this.parseBlock = block
    }

    fun build(): StatementRule<T> {
        val n = checkNotNull(name.takeIf { it.isNotBlank() }) { "Rule name must be specified via withName()" }
        val s = checkNotNull(startsPredicate) { "starts predicate must be defined for rule '$n'" }
        val p = checkNotNull(parseBlock) { "parse block must be defined for rule '$n'" }
        return SimpleStatementRule(n, s, p)
    }
}

class SimpleStatementRule<T : Statement>(
    override val tag: String,
    private val startsPredicate: (Cursor<Token>) -> Boolean,
    private val parseBlock: (Cursor<Token>, ExpressionParser, (Cursor<Token>) -> Result<Statement>) -> Result<T>
) : StatementRule<T> {
    override fun canStart(cursor: Cursor<Token>): Boolean = startsPredicate(cursor)
    override fun parse(
        cursor: Cursor<Token>,
        expressionParser: ExpressionParser,
        nextStatement: (Cursor<Token>) -> Result<Statement>
    ): Result<T> = parseBlock(cursor, expressionParser, nextStatement)
}
