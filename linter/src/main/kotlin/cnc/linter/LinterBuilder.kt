package cnc.linter

import cnc.common.ConfigReader
import cnc.common.ErrorType
import cnc.common.Failure
import cnc.common.NamingValidator
import cnc.common.Result
import cnc.common.Success
import com.google.gson.Gson
import com.google.gson.JsonObject
import com.google.gson.JsonPrimitive

/** Builder fluido del linter: `LinterBuilder.default().addValidator(...).build(json)`. */
class LinterBuilder private constructor(
    private val validators: ValidatorsContainer,
) {
    /** Devuelve una copia con [validator] registrado bajo [key]. */
    fun addValidator(
        key: String,
        validator: NamingValidator,
    ): LinterBuilder = LinterBuilder(validators.withValidator(key, validator))

    /** Arma el [CNCLinter] según el JSON de configuración. */
    fun build(jsonString: String): Result<CNCLinter> {
        val root =
            runCatching {
                Gson().fromJson(jsonString, JsonObject::class.java)
            }.getOrNull() ?: JsonObject()
        val reader = ConfigReader.fromMap(root.toValueMap())

        val rules = mutableListOf<LinterRule>()

        val format = reader.readString(namingConventionKeys)
        if (format != null) {
            val validator =
                validators.resolve(format)
                    ?: return Failure("Convention validator for '$format' not found", ErrorType.CLI)
            rules.add(NamingConventionRule(format, validator))
        }

        if (reader.isEnabled(printlnRuleKeys)) {
            rules.add(SimpleFunctionCallRule("println"))
        }

        if (reader.isEnabled(readInputRuleKeys)) {
            rules.add(SimpleFunctionCallRule("readInput"))
        }

        return Success("ok", CNCLinter(rules))
    }

    companion object {
        private val namingConventionKeys = listOf("identifier_format", "identifier-format", "naming-convention")
        private val printlnRuleKeys = listOf("mandatory-variable-or-literal-in-println", "simple-println")
        private val readInputRuleKeys =
            listOf(
                "mandatory-variable-or-literal-in-readInput",
                "mandatory-variable-or-literal-in-read-input",
                "simple-read-input",
            )

        /** Builder vacío (sin validadores registrados). */
        fun empty(): LinterBuilder = LinterBuilder(ValidatorsContainer.empty())

        /** Builder con los validadores estándar (camelCase, snake_case). */
        fun default(): LinterBuilder = LinterBuilder(ValidatorsContainer.default())

        /** Puente gson -> mapa plano de nivel superior para [ConfigReader]. */
        private fun JsonObject.toValueMap(): Map<String, Any?> =
            entrySet().associate { (key, element) ->
                val value: Any? =
                    when {
                        element == null || element.isJsonNull -> null
                        element is JsonPrimitive && element.isBoolean -> element.asBoolean
                        element is JsonPrimitive && element.isNumber -> element.asNumber
                        element is JsonPrimitive -> element.asString
                        else -> element.toString()
                    }
                key to value
            }
    }
}
