package cnc.formatter

import cnc.ast.Expression

/**
 * Regla de formato de expresiones con **self-dispatch** (Decisión 9).
 *
 * En lugar de mapear por tipo (`Map<KClass, ...>`, requiere cast) o enumerar los
 * tipos (visitor/data class, cerrado), cada regla se autoevalúa: intenta
 * formatear el nodo y devuelve `null` si no le corresponde. El motor prueba las
 * reglas en orden y usa la primera que devuelve no-null (chain of
 * responsibility). Extender = agregar una regla a la lista, sin tocar clases.
 *
 * Precio asumido: no hay exhaustividad en compilación. Si ningún
 * [ExpressionRule] aplica a un nodo, falla en runtime.
 */
fun interface ExpressionRule {
    /** Devuelve el render de [expr], o `null` si esta regla no le aplica. */
    fun tryFormat(expr: Expression, context: FormatContext): String?
}
