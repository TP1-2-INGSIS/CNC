package cnc.formatter

/**
 * Abstracción unificada de una regla de formato (Decisión 7): toma un nodo (una
 * [cnc.ast.Expression] concreta) o un símbolo (`String`) y produce texto.
 * Los statements se formatean vía [StatementRule] (self-dispatch). Las reglas
 * concretas se inyectan desde `app/config/` (Decisión 8).
 */
fun interface FormatRule<T> {
    fun format(node: T, context: FormatContext): String
}
