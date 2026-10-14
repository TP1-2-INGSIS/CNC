package cnc.parser

import cnc.ast.BlockStatement
import cnc.ast.Expression
import cnc.ast.Statement
import cnc.common.Cursor
import cnc.common.ErrorType
import cnc.common.Failure
import cnc.common.Result
import cnc.common.Success
import cnc.common.asCursor
import cnc.parser.cursor.flow
import cnc.parser.expression.ExpressionParser
import cnc.parser.flow.FailedFlow
import cnc.parser.rule.StatementParser
import cnc.parser.rule.StatementRule
import cnc.token.Token
import cnc.token.TokenDefinition

/**
 * Predictive, streaming parser for statements.
 */
class Parser(
    val rules: List<StatementRule<Statement>>,
    val expressionParser: ExpressionParser,
    val blockOpen: TokenDefinition? = null,
    val blockClose: TokenDefinition? = null,
) : StatementParser {
    constructor(
        vararg rules: StatementRule<Statement>,
        expressionParser: ExpressionParser,
    ) : this(rules.toList(), expressionParser)

    constructor(
        vararg rules: StatementRule<Statement>,
        expressionParser: ExpressionParser,
        blockOpen: TokenDefinition?,
        blockClose: TokenDefinition?,
    ) : this(rules.toList(), expressionParser, blockOpen, blockClose)

    /**
     * Parses a lazy sequence of [Token]s into a lazy sequence of [Result] containing [Statement]s.
     * Operates continuously with fail-fast semantics on syntax errors.
     */
    fun parse(tokens: Sequence<Token>): Sequence<Result<Statement>> =
        sequence {
            val cursor = tokens.asCursor()
            while (cursor.hasMore()) {
                val result = parseStatement(cursor)
                yield(result)
                if (result is Failure) break
            }
        }

    override fun parseExpression(cursor: Cursor<Token>): Result<Expression> = expressionParser.parse(cursor)

    override fun parseStatement(cursor: Cursor<Token>): Result<Statement> {
        val rule =
            rules.firstOrNull { it.canStart(cursor) }
                ?: return unexpectedToken(cursor.advance())

        return rule.parse(cursor, this)
    }

    override fun parseBlock(cursor: Cursor<Token>): Result<BlockStatement> {
        val openDef =
            blockOpen
                ?: return Failure("No block open delimiter configured in Parser", ErrorType.PARSER)
        val closeDef =
            blockClose
                ?: return Failure("No block close delimiter configured in Parser", ErrorType.PARSER)

        val openFlow = cursor.flow().expect(openDef)
        if (openFlow is FailedFlow) return Failure(openFlow.failure.msg, openFlow.failure.type)

        val statements = mutableListOf<Statement>()
        while (cursor.hasMore() && cursor.peek()?.let { closeDef.match(it.text) } != true) {
            when (val res = parseStatement(cursor)) {
                is Success -> statements.add(res.data)
                is Failure -> return Failure(res.msg, res.type)
            }
        }

        val closeFlow = cursor.flow().expect(closeDef)
        if (closeFlow is FailedFlow) return Failure(closeFlow.failure.msg, closeFlow.failure.type)

        return Success("Parsed block", BlockStatement(statements))
    }

    private fun unexpectedToken(token: Token?): Failure<Statement> {
        val pos = token?.pos?.let { " at row ${it.row}, col ${it.col}" } ?: ""
        return Failure("Syntax error: unexpected token '${token?.text}'$pos", ErrorType.PARSER)
    }

    fun getASTs(tokens: Sequence<Token>): Sequence<Result<Statement>> = parse(tokens)

    fun copy(
        rules: List<StatementRule<Statement>> = this.rules,
        expressionParser: ExpressionParser = this.expressionParser,
        blockOpen: TokenDefinition? = this.blockOpen,
        blockClose: TokenDefinition? = this.blockClose,
    ): Parser = Parser(rules, expressionParser, blockOpen, blockClose)
}
