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
        declarationTag: String
    ): CNCLinter {

        val config = Gson().fromJson(jsonString, LinterConfigDto::class.java)
            ?: LinterConfigDto()

        val activeRules = mutableListOf<LinterRule>()

        if (config.namingConvention != null) {
            val convention = when (config.namingConvention) {
                "camelCase" -> NamingConvention.CAMEL_CASE
                "snake_case" -> NamingConvention.SNAKE_CASE
                else -> throw IllegalArgumentException("Convención desconocida: \${config.namingConvention}")
            }
            activeRules.add(NamingConventionRule(declarationTag, convention))
        }

        return CNCLinter(activeRules)
    }
}
