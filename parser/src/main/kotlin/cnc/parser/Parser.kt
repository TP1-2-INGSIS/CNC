package cnc.parser

import cnc.ast.Statement
import cnc.common.Cursor
import cnc.common.ErrorType
import cnc.common.Failure
import cnc.common.Result
import cnc.common.asCursor
import cnc.parser.expression.ExpressionParser
import cnc.parser.rule.StatementRule
import cnc.token.Token

/**
 * Predictive, streaming parser for PrintScript statements.
 */
class Parser(
    val rules: List<StatementRule<Statement>>,
    val expressionParser: ExpressionParser
) {

    constructor(
        vararg rules: StatementRule<Statement>,
        expressionParser: ExpressionParser
    ) : this(rules.toList(), expressionParser)

    /**
     * Parses a lazy sequence of [Token]s into a lazy sequence of [Result] containing [Statement]s.
     * Operates continuously with fail-fast semantics on syntax errors.
     */
    fun parse(tokens: Sequence<Token>): Sequence<Result<Statement>> = sequence {
        val cursor = tokens.asCursor()
        while (cursor.hasMore()) {
            val result = nextStatement(cursor)
            yield(result)
            if (result is Failure) break
        }
    }

    private fun nextStatement(cursor: Cursor<Token>): Result<Statement> {
        val rule = rules.firstOrNull { it.canStart(cursor) }
            ?: return unexpectedToken(cursor.advance())

        return rule.parse(cursor, expressionParser)
    }

    private fun unexpectedToken(token: Token?): Failure<Statement> {
        val pos = token?.pos?.let { " at row ${it.row}, col ${it.col}" } ?: ""
        return Failure("Syntax error: unexpected token '${token?.text}'$pos", ErrorType.PARSER)
    }

    fun getASTs(tokens: Sequence<Token>): Sequence<Result<Statement>> = parse(tokens)

    fun copy(
        rules: List<StatementRule<Statement>> = this.rules,
        expressionParser: ExpressionParser = this.expressionParser
    ): Parser = Parser(rules, expressionParser)
}
