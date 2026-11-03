package cnc.formatter

import cnc.ast.Statement
import cnc.common.ErrorType
import cnc.common.Failure
import cnc.common.Result
import cnc.common.Success
import com.google.gson.Gson
import com.google.gson.JsonObject

/** Fábrica de [Formatter]: resuelve el JSON a un [FormatConfig] y arma el formatter con [StandardFormatRules]. */
object FormatterFactory {
    fun build(jsonString: String?): Result<Formatter> {
        if (jsonString.isNullOrBlank()) {
            return Success("ok", StandardFormatRules.defaultFormatter())
        }

        val root =
            runCatching {
                Gson().fromJson(jsonString, JsonObject::class.java)
            }.getOrNull() ?: return Failure("Invalid formatter JSON configuration", ErrorType.CLI)

        val config = FormatConfig.from(root)
        return Success("ok", StandardFormatRules.formatterFor(config))
    }

    fun create(jsonString: String? = null): Formatter =
        when (val result = build(jsonString)) {
            is Success -> result.data
            is Failure -> StandardFormatRules.defaultFormatter()
        }

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
