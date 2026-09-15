package cnc.formatter

import cnc.ast.BinaryExpression
import cnc.ast.Expression
import cnc.ast.Statement

/**
 * Motor de formateo genérico. No conoce PrintScript: recibe las reglas
 * concretas inyectadas desde `app/config/` (Decisiones 7 y 8).
 *
 * - Fase general: despacha statements por self-dispatch ([StatementRule]).
 * - Fase specific: resuelve símbolos y expresiones vía [FormatContext].
 *
 * Los paréntesis se reintroducen por precedencia (Decisión 10) usando [precedences].
 */
class Formatter(
    private val statementRules: List<StatementRule>,
    private val symbolRules: Map<String, FormatRule<String>>,
    private val expressionRules: List<ExpressionRule>,
    private val precedences: Map<String, Int> = emptyMap()
) {

    private val context = object : FormatContext {
        override fun formatStatement(statement: Statement): String =
            statementRules.firstNotNullOfOrNull { it.tryFormat(statement, this) }
                ?: error("No StatementRule applies to '$statement'")

        override fun formatSymbol(symbol: String): String {
            val rule = symbolRules[symbol]
                ?: error("No FormatRule registered for symbol '$symbol'")
            return rule.format(symbol, this)
        }

        override fun formatExpression(expression: Expression): String =
            expressionRules.firstNotNullOfOrNull { it.tryFormat(expression, this) }
                ?: error("No ExpressionRule applies to '$expression'")

        override fun formatOperand(
            child: Expression,
            parentPrecedence: Int,
            side: OperandSide
        ): String {
            val rendered = formatExpression(child)
            return if (needsParentheses(child, parentPrecedence, side)) "($rendered)" else rendered
        }
    }

    // Un operando binario se envuelve si su precedencia es menor que la del padre.
    private fun needsParentheses(
        child: Expression,
        parentPrecedence: Int,
        @Suppress("UNUSED_PARAMETER") side: OperandSide
    ): Boolean {
        val binary = child as? BinaryExpression ?: return false
        val childPrecedence = precedences[binary.operator]
            ?: error("No precedence registered for operator '${binary.operator}'")
        return childPrecedence < parentPrecedence
    }

    /** Formatea el AST completo a su forma textual canónica. */
    fun format(ast: List<Statement>): String =
        ast.joinToString(separator = "\n") { statement ->
            statementRules.firstNotNullOfOrNull { it.tryFormat(statement, context) }
                ?: error("No StatementRule applies to '$statement'")
        }
}
