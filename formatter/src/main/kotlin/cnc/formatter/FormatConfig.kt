package cnc.formatter

import cnc.common.ConfigReader
import com.google.gson.JsonObject
import com.google.gson.JsonPrimitive

/** Espaciado a aplicar alrededor de un símbolo (`=`, `:`, operadores). */
enum class Spacing {
    AROUND,
    BEFORE,
    AFTER,
    NONE,
    ;

    fun apply(symbol: String): String =
        when (this) {
            AROUND -> " $symbol "
            BEFORE -> " $symbol"
            AFTER -> "$symbol "
            NONE -> symbol
        }
}

/** Configuración de formato ya resuelta a decisiones concretas; única fuente de verdad de las opciones del JSON. */
data class FormatConfig(
    val equalsSpacing: Spacing = Spacing.AROUND,
    val colonSpacing: Spacing = Spacing.AFTER,
    val operatorSpacing: Spacing = Spacing.AROUND,
    val singleSpaceSeparation: Boolean = false,
    val lineBreaksAfterPrintln: Int = 0,
    val ifBraceBelowLine: Boolean = false,
    val indentInsideIf: Int = DEFAULT_INDENT,
) {
    companion object {
        /** Indentación por defecto sin JSON de configuración. */
        const val DEFAULT_INDENT = 4

        /** Indentación cuando hay JSON pero sin la clave `indent-inside-if` (comportamiento histórico del factory). */
        const val JSON_FALLBACK_INDENT = 2

        val DEFAULT = FormatConfig()

        private val equalsNoSpaceAroundKeys = listOf("enforce-no-spacing-around-equals", "no-space-around-equals")
        private val colonBeforeKeys = listOf("enforce-spacing-before-colon-in-declaration", "space-before-colon")
        private val colonAfterKeys = listOf("enforce-spacing-after-colon-in-declaration", "space-after-colon")
        private val singleSpaceSeparationKeys = listOf("mandatory-single-space-separation", "single-space-separation")
        private val printlnLineBreaksKeys = listOf("line-breaks-after-println", "line-break-after-println")
        private val ifBraceBelowLineKeys = listOf("if-brace-below-line")
        private val indentInsideIfKeys = listOf("indent-inside-if")

        /** Resuelve un [FormatConfig] desde el JSON; un `json` nulo produce [DEFAULT]. */
        fun from(json: JsonObject?): FormatConfig {
            if (json == null) return DEFAULT
            return from(ConfigReader.fromMap(json.toValueMap()))
        }

        /** Resuelve un [FormatConfig] desde un [ConfigReader] ya construido. */
        fun from(reader: ConfigReader): FormatConfig {
            val noSpaceAroundEquals = reader.isEnabled(equalsNoSpaceAroundKeys)
            val spaceBeforeColon = reader.isEnabled(colonBeforeKeys)
            val spaceAfterColon = reader.isEnabled(colonAfterKeys)
            val singleSpace = reader.isEnabled(singleSpaceSeparationKeys)

            val equalsSpacing = if (noSpaceAroundEquals) Spacing.NONE else Spacing.AROUND

            val colonSpacing =
                when {
                    singleSpace -> Spacing.AROUND
                    spaceBeforeColon && spaceAfterColon -> Spacing.AROUND
                    spaceBeforeColon -> Spacing.BEFORE
                    spaceAfterColon -> Spacing.AFTER
                    else -> Spacing.AFTER
                }

            return FormatConfig(
                equalsSpacing = equalsSpacing,
                colonSpacing = colonSpacing,
                operatorSpacing = Spacing.AROUND,
                singleSpaceSeparation = singleSpace,
                lineBreaksAfterPrintln = reader.readInt(printlnLineBreaksKeys) ?: 0,
                ifBraceBelowLine = reader.isEnabled(ifBraceBelowLineKeys),
                indentInsideIf = reader.readInt(indentInsideIfKeys) ?: JSON_FALLBACK_INDENT,
            )
        }

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
