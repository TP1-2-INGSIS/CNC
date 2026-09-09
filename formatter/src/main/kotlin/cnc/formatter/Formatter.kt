package cnc.formatter

import cnc.ast.BinaryExpression
import cnc.ast.Expression
import cnc.ast.GenericStatement

/**
 * Motor de formateo genérico. No conoce PrintScript: recibe inyectadas las
 * reglas concretas desde `app/config/` como código Kotlin (Decisiones 7 y 8).
 *
 * Formatea en una sola pasada con dos niveles anidados (Decisión 5):
 *  - Fase `general` (externa): recorre los statements y despacha por `tag`.
 *  - Fase `specific` (interna): mientras la regla de statement construye la
 *    forma, resuelve símbolos (`formatSymbol`) y expresiones (`formatExpression`)
 *    vía el [FormatContext].
 *
 * Las expresiones usan self-dispatch por lista (Decisión 9). Los paréntesis se
 * reintroducen por precedencia vía [FormatContext.formatOperand] (Decisión 10),
 * usando la tabla [precedences] inyectada.
 *
 * @property statementRules  mapa `tag` -> regla estructural (fase general).
 * @property symbolRules     mapa símbolo -> regla de símbolo (fase specific).
 * @property expressionRules lista de reglas de expresión (chain of responsibility).
 * @property precedences     tabla operador -> precedencia (para parentización).
 */
class Formatter(
    private val statementRules: Map<String, FormatRule<GenericStatement>>,
    private val symbolRules: Map<String, FormatRule<String>>,
    private val expressionRules: List<ExpressionRule>,
    private val precedences: Map<String, Int> = emptyMap()
) {

    private val context = object : FormatContext {
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

    /**
     * Criterio de parentización (Decisión 10): un operando binario se envuelve si
     * su precedencia es **menor** que la del padre. En empate no se envuelve
     * (se asume asociatividad izquierda estándar, coherente con la mayoría de los
     * operadores). Operandos no-binarios (literales, identificadores) nunca se
     * envuelven.
     */
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
    fun format(ast: List<GenericStatement>): String =
        ast.joinToString(separator = "\n") { statement ->
            val rule = statementRules[statement.tag]
                ?: error("No FormatRule registered for tag '${statement.tag}'")
            rule.format(statement, context)
        }
}
