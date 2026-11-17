package cnc.linter

import cnc.ast.Expression
import cnc.ast.Statement
import kotlin.reflect.KClass

/**
 * Motor genérico e inmutable del linter.
 * Despacha sentencias y expresiones en O(1) según su clase concreta del AST,
 * sin acoplarse a ningún subtipo concreto de [Statement] o [Expression].
 */
class CNCLinter(
    private val statementRules: Map<KClass<out Statement>, StatementLintRule<*>> = emptyMap(),
    private val expressionRules: Map<KClass<out Expression>, ExpressionLintRule<*>> = emptyMap(),
) {
    @Suppress("UNCHECKED_CAST")
    fun lint(statement: Statement): List<String> {
        val rule =
            statementRules[statement::class] as? StatementLintRule<Statement>
                ?: return emptyList()
        return rule.lint(statement, this)
    }

    @Suppress("UNCHECKED_CAST")
    fun lint(expression: Expression): List<String> {
        val rule =
            expressionRules[expression::class] as? ExpressionLintRule<Expression>
                ?: return emptyList()
        return rule.lint(expression, this)
    }

    fun lint(statements: Sequence<Statement>): List<String> = statements.flatMap { lint(it).asSequence() }.toList()

    companion object {
        fun builder(): LinterBuilder = LinterBuilder()
    }
}
