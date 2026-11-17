package cnc.linter

import cnc.ast.Expression
import cnc.ast.Statement
import cnc.common.ConfigReader
import cnc.common.Result
import cnc.common.Success

/** Contrato para lintear una sentencia de tipo [T]. */
interface StatementLintRule<T : Statement> {
    fun lint(
        statement: T,
        linter: CNCLinter,
    ): List<String>

    fun configure(reader: ConfigReader): Result<StatementLintRule<T>> = Success("ok", this)
}

/** Contrato para lintear una expresión de tipo [T]. */
interface ExpressionLintRule<T : Expression> {
    fun lint(
        expression: T,
        linter: CNCLinter,
    ): List<String>

    fun configure(reader: ConfigReader): Result<ExpressionLintRule<T>> = Success("ok", this)
}
