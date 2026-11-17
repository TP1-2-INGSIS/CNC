package cnc.linter

import cnc.ast.BooleanLiteral
import cnc.ast.Call
import cnc.ast.CallExpression
import cnc.ast.Expression
import cnc.ast.Identifier
import cnc.ast.NumberLiteral
import cnc.ast.StringLiteral
import cnc.common.ConfigReader
import cnc.common.Result
import cnc.common.Success

private fun isVariableOrLiteral(expr: Expression): Boolean =
    expr is Identifier || expr is NumberLiteral || expr is StringLiteral || expr is BooleanLiteral

private fun checkCallArguments(
    targetFunction: String,
    calledFunction: String,
    arguments: List<Expression>,
    enabled: Boolean,
    linter: CNCLinter,
): List<String> {
    val warnings = mutableListOf<String>()
    if (enabled && calledFunction == targetFunction) {
        for (arg in arguments) {
            if (!isVariableOrLiteral(arg)) {
                warnings.add("La llamada a '$targetFunction' solo permite una variable o un literal como argumento.")
            }
        }
    }
    for (arg in arguments) {
        warnings.addAll(linter.lint(arg))
    }
    return warnings
}

/** Valida que las llamadas sentencia a [functionName] reciban únicamente variables o literales y recorre sus argumentos. */
data class CallStatementLintRule(
    val functionName: String = "println",
    val enabled: Boolean = false,
    val aliases: List<String> = DEFAULT_PRINTLN_ALIASES,
) : StatementLintRule<Call> {
    override fun lint(
        statement: Call,
        linter: CNCLinter,
    ): List<String> = checkCallArguments(functionName, statement.function, statement.arguments, enabled, linter)

    override fun configure(reader: ConfigReader): Result<StatementLintRule<Call>> =
        Success("ok", copy(enabled = reader.isEnabled(aliases)))

    companion object {
        val DEFAULT_PRINTLN_ALIASES: List<String> =
            listOf("mandatory-variable-or-literal-in-println", "simple-println")
    }
}

/** Valida que las llamadas expresión a [functionName] reciban únicamente variables o literales y recorre sus argumentos. */
data class CallExpressionLintRule(
    val functionName: String = "readInput",
    val enabled: Boolean = false,
    val aliases: List<String> = DEFAULT_READ_INPUT_ALIASES,
) : ExpressionLintRule<CallExpression> {
    override fun lint(
        expression: CallExpression,
        linter: CNCLinter,
    ): List<String> = checkCallArguments(functionName, expression.function, expression.arguments, enabled, linter)

    override fun configure(reader: ConfigReader): Result<ExpressionLintRule<CallExpression>> =
        Success("ok", copy(enabled = reader.isEnabled(aliases)))

    companion object {
        val DEFAULT_READ_INPUT_ALIASES: List<String> =
            listOf(
                "mandatory-variable-or-literal-in-readInput",
                "mandatory-variable-or-literal-in-read-input",
                "simple-read-input",
            )
    }
}
