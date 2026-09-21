package cnc.linter

import cnc.common.ErrorType
import cnc.common.Failure
import cnc.common.Result
import cnc.common.Success
import com.google.gson.Gson
import com.google.gson.JsonObject

object LinterFactory {

    private val namingConventionKeys = listOf("identifier_format", "identifier-format", "naming-convention")
    private val printlnRuleKeys = listOf("mandatory-variable-or-literal-in-println", "simple-println")
    private val readInputRuleKeys = listOf("mandatory-variable-or-literal-in-readInput", "mandatory-variable-or-literal-in-read-input", "simple-read-input")

    fun build(
        jsonString: String,
        validators: Map<String, NamingValidator>
    ): Result<CNCLinter> {
        val root = runCatching {
            Gson().fromJson(jsonString, JsonObject::class.java)
        }.getOrNull() ?: JsonObject()

        val rules = mutableListOf<LinterRule>()

        // 1. Convención de nombres de variables
        val format = readStringProperty(root, namingConventionKeys)
        if (format != null) {
            when (val validatorResult = resolveNamingValidator(format, validators)) {
                is Failure -> return Failure(validatorResult.msg, validatorResult.type)
                is Success -> rules.add(NamingConventionRule(format, validatorResult.data))
            }
        }

        // 2. Restricción de argumento en println (solo variable o literal)
        if (isRuleEnabled(root, printlnRuleKeys)) {
            rules.add(SimpleFunctionCallRule("println"))
        }

        // 3. Restricción de prompt en readInput (solo variable o literal)
        if (isRuleEnabled(root, readInputRuleKeys)) {
            rules.add(SimpleFunctionCallRule("readInput"))
        }

        return Success("ok", CNCLinter(rules))
    }

    /**
     * Lee la primera propiedad String que exista en el JSON según la lista de nombres posibles (aliases).
     */
    private fun readStringProperty(json: JsonObject, candidateKeys: List<String>): String? {
        val matchingKey = candidateKeys.firstOrNull { json.has(it) && !json.get(it).isJsonNull } ?: return null
        return json.get(matchingKey).asString.trim()
    }

    /**
     * Comprueba si una regla booleana está activada (true) para cualquiera de sus nombres posibles en el JSON.
     */
    private fun isRuleEnabled(json: JsonObject, candidateKeys: List<String>): Boolean {
        val matchingKey = candidateKeys.firstOrNull { json.has(it) && !json.get(it).isJsonNull } ?: return false
        return json.get(matchingKey).asBoolean
    }

    private fun resolveNamingValidator(format: String, validators: Map<String, NamingValidator>): Result<NamingValidator> {
        val normalized = when {
            format.contains("camel", ignoreCase = true) -> "camelCase"
            format.contains("snake", ignoreCase = true) -> "snake_case"
            else -> format
        }
        val validator = validators[normalized] ?: validators[format]
            ?: return Failure("Convention validator for '$format' not found", ErrorType.CLI)
        return Success("ok", validator)
    }
}
