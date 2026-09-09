package cnc.formatter

/**
 * Abstracción unificada de una regla de formato (Decisión 7).
 *
 * Toda regla toma un pedazo del AST (un [cnc.ast.GenericStatement], una
 * [cnc.ast.Expression] concreta) o un símbolo (`String`), junto con un
 * [FormatContext] para componer sub-render, y produce texto formateado.
 *
 * - `FormatRule<GenericStatement>` → regla estructural, mapeada por `tag`.
 * - `FormatRule<String>`           → regla de símbolo/operador (el nodo es el
 *   texto del símbolo), mapeada por símbolo.
 * - `FormatRule<NumberLiteral>`, `FormatRule<BinaryExpression>`, ... → reglas de
 *   expresión por tipo.
 *
 * El motor `:formatter` no conoce las reglas concretas: se inyectan desde
 * `app/config/` como código Kotlin (Decisión 8).
 */
fun interface FormatRule<T> {
    fun format(node: T, context: FormatContext): String
}
