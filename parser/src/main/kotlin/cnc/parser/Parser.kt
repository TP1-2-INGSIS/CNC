package cnc.parser

import cnc.ast.Statement
import cnc.common.Cursor
import cnc.common.ErrorType
import cnc.common.Failure
import cnc.common.Result
import cnc.common.Success
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
     * Operates continuously without blind delimiter slicing.
     */
    fun parse(tokens: Sequence<Token>): Sequence<Result<Statement>> = sequence {
        val cursor = tokens.asCursor()
        while (cursor.hasMore()) {
            val rule = rules.firstOrNull { it.canStart(cursor) }
            if (rule != null) {
                val result = rule.parse(cursor, expressionParser)
                yield(result)
                if (result is Failure) {
                    // Synchronize: skip tokens up to ';' to recover for next statement
                    while (cursor.hasMore()) {
                        if (cursor.advance()?.text == ";") break
                    }
                }
            } else {
                val unexpected = cursor.peek()
                val pos = unexpected?.pos
                val posStr = if (pos != null) " at row ${pos.row}, col ${pos.col}" else ""
                yield(
                    Failure("Syntax error: unexpected token '${unexpected?.text}'$posStr", ErrorType.PARSER)
                )
                cursor.advance() // Discard unexpected token to recover stream
            }
        }

    }

    /**
     * Convenience method: parses and returns statements, throwing ParseException on error.
     */
    fun getASTs(tokens: Sequence<Token>): Sequence<Statement> = parse(tokens).map { result ->
        when (result) {
            is Success -> result.data
            is Failure -> throw ParseException(result.msg)
        }
    }

    fun copy(
        rules: List<StatementRule<Statement>> = this.rules,
        expressionParser: ExpressionParser = this.expressionParser
    ): Parser = Parser(rules, expressionParser)
}

/**
 * Error de parsing con información de posición y contexto.
 */
class ParseException(
    message: String,
    val token: Token? = null
) : RuntimeException(message)
