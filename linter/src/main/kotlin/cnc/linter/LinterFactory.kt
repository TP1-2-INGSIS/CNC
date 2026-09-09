package cnc.linter

import com.google.gson.Gson
import com.google.gson.annotations.SerializedName

data class LinterConfigDto(
    @SerializedName("naming-convention") val namingConvention: String? = null,
    @SerializedName("simple-println") val simplePrintln: Boolean = false
)

object LinterFactory {

    fun build(
        jsonString: String,
        declarationTag: String,
        validators: Map<String, NamingValidator>
    ): CNCLinter {

        val config = Gson().fromJson(jsonString, LinterConfigDto::class.java)
            ?: LinterConfigDto()

        val activeRules = mutableListOf<LinterRule>()

        if (config.namingConvention != null) {
            val validator = validators[config.namingConvention]
                ?: throw IllegalArgumentException("Convention validator for '${config.namingConvention}' not found")
            activeRules.add(NamingConventionRule(declarationTag, config.namingConvention, validator))
        }

        return CNCLinter(activeRules)
    }
}
