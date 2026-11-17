package cnc.formatter

import cnc.ast.Statement
import cnc.common.Result

/** Fábrica de [Formatter]: delega en [StandardFormatRules.defaultBuilder]. */
object FormatterFactory {
    fun build(jsonString: String?): Result<Formatter> = StandardFormatRules.defaultBuilder().build(jsonString)

    fun create(jsonString: String? = null): Formatter = StandardFormatRules.defaultBuilder().create(jsonString)

    /** Formatea el fuente vía AST; si [parse] no produce statements, devuelve el texto crudo (A2 tolerante). */
    fun formatSource(
        rawSource: String,
        configJson: String?,
        parse: (String) -> List<Statement>?,
    ): String {
        val statements = runCatching { parse(rawSource) }.getOrNull() ?: return rawSource
        return create(configJson).format(statements)
    }
}
