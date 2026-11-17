package cnc.formatter

import cnc.ast.Expression
import cnc.ast.Statement
import kotlin.reflect.KClass

/**
 * Motor de formateo genérico e inmutable.
 * Despacha sentencias y expresiones en O(1) según su clase concreta del AST.
 */
class Formatter(
    private val statementRules: Map<KClass<out Statement>, StatementFormatRule<*>>,
    private val expressionRules: Map<KClass<out Expression>, ExpressionFormatRule<*>>,
    private val statementSeparator: String = "\n",
) {
    @Suppress("UNCHECKED_CAST")
    fun format(statement: Statement): String {
        val rule =
            statementRules[statement::class] as? StatementFormatRule<Statement>
                ?: error("No StatementFormatRule registered for '${statement::class.simpleName}'")
        return rule.format(statement, this)
    }

    @Suppress("UNCHECKED_CAST")
    fun format(expression: Expression): String {
        val rule =
            expressionRules[expression::class] as? ExpressionFormatRule<Expression>
                ?: error("No ExpressionFormatRule registered for '${expression::class.simpleName}'")
        return rule.format(expression, this)
    }

    /** Formatea una colección de sentencias a su representación textual canónica. */
    fun format(statements: Iterable<Statement>): String =
        statements.joinToString(separator = statementSeparator) { format(it) }

    companion object {
        fun builder(): FormatterBuilder = FormatterBuilder()
    }
}
