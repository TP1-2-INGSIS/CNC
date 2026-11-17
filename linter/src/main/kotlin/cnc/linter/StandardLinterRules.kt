package cnc.linter

import cnc.ast.Assignment
import cnc.ast.BinaryExpression
import cnc.ast.BlockStatement
import cnc.ast.IfStatement
import cnc.ast.UnaryExpression

/** Regla estructural para asignaciones: recorre su expresión de valor. */
object AssignmentLintRule : StatementLintRule<Assignment> {
    override fun lint(
        statement: Assignment,
        linter: CNCLinter,
    ): List<String> = linter.lint(statement.value)
}

/** Regla estructural para bloques: recorre cada sentencia interna. */
object BlockStatementLintRule : StatementLintRule<BlockStatement> {
    override fun lint(
        statement: BlockStatement,
        linter: CNCLinter,
    ): List<String> = statement.statements.flatMap { linter.lint(it) }
}

/** Regla estructural para condicionales `if` / `else`: recorre condición y bloques. */
object IfStatementLintRule : StatementLintRule<IfStatement> {
    override fun lint(
        statement: IfStatement,
        linter: CNCLinter,
    ): List<String> {
        val warnings = mutableListOf<String>()
        warnings.addAll(linter.lint(statement.condition))
        warnings.addAll(linter.lint(statement.thenBlock))
        statement.elseBlock?.let { warnings.addAll(linter.lint(it)) }
        return warnings
    }
}

/** Regla estructural para expresiones binarias: recorre ambos operandos. */
object BinaryExpressionLintRule : ExpressionLintRule<BinaryExpression> {
    override fun lint(
        expression: BinaryExpression,
        linter: CNCLinter,
    ): List<String> = linter.lint(expression.left) + linter.lint(expression.right)
}

/** Regla estructural para expresiones unarias: recorre su operando. */
object UnaryExpressionLintRule : ExpressionLintRule<UnaryExpression> {
    override fun lint(
        expression: UnaryExpression,
        linter: CNCLinter,
    ): List<String> = linter.lint(expression.operand)
}
