package cnc.printscript.format

import cnc.common.ConfigReader
import com.google.gson.Gson
import com.google.gson.JsonObject
import com.google.gson.JsonPrimitive

/**
 * Formateador preservador: parte del texto original y aplica únicamente las reglas
 * activas en la config, preservando todo lo demás (incluidos los saltos de línea
 * entre statements y la ausencia de trailing newline). Es el modelo del TCK.
 */
object SourceFormatter {
    private val equalsSpaceKeys = listOf("enforce-spacing-around-equals", "space-around-equals")
    private val equalsNoSpaceKeys = listOf("enforce-no-spacing-around-equals", "no-space-around-equals")
    private val colonBeforeKeys = listOf("enforce-spacing-before-colon-in-declaration", "space-before-colon")
    private val colonAfterKeys = listOf("enforce-spacing-after-colon-in-declaration", "space-after-colon")
    private val singleSpaceKeys = listOf("mandatory-single-space-separation", "single-space-separation")
    private val spaceOpsKeys = listOf("mandatory-space-surrounding-operations", "enforce-spacing-around-operations")
    private val lineBreakStmtKeys = listOf("mandatory-line-break-after-statement", "line-break-after-statement")
    private val printlnBreaksKeys = listOf("line-breaks-after-println", "line-break-after-println")
    private val ifBraceBelowKeys = listOf("if-brace-below-line")
    private val ifBraceSameKeys = listOf("if-brace-same-line")
    private val indentInsideIfKeys = listOf("indent-inside-if")

    fun format(
        source: String,
        configJson: String?,
    ): String {
        if (configJson.isNullOrBlank()) return source
        val root = runCatching { Gson().fromJson(configJson, JsonObject::class.java) }.getOrNull() ?: return source
        val reader = ConfigReader.fromMap(root.toValueMap())
        if (SourceTokenizer.tokenize(source).none { it.kind != TokenKind.WHITESPACE }) return source

        val cfg = SpacingFlags(reader)

        // Fase 1: espaciado horizontal (componible: todas las reglas activas en una pasada).
        var text =
            if (reader.isEnabled(singleSpaceKeys)) {
                singleSpace(SourceTokenizer.tokenize(source))
            } else if (cfg.anyGapRule) {
                rewriteGaps(SourceTokenizer.tokenize(source)) { prev, next, gap -> cfg.decide(prev, next, gap) }
            } else {
                source
            }

        // Fase 2: estructura de líneas / bloques (una por config, como define el TCK).
        text =
            when {
                reader.readInt(indentInsideIfKeys) != null -> indentInsideIf(text, reader.readInt(indentInsideIfKeys)!!)
                reader.isEnabled(ifBraceBelowKeys) -> ifBrace(text, below = true)
                reader.isEnabled(ifBraceSameKeys) -> ifBrace(text, below = false)
                reader.readInt(printlnBreaksKeys) != null -> printlnBreaks(text, reader.readInt(printlnBreaksKeys)!!)
                reader.isEnabled(lineBreakStmtKeys) -> lineBreakAfterStatement(text)
                else -> text
            }

        return text
    }

    /** Reglas de espaciado horizontal activas, aplicadas en conjunto sobre cada gap. */
    private class SpacingFlags(
        reader: ConfigReader,
    ) {
        private val equalsNoSpace = reader.isEnabled(equalsNoSpaceKeys)
        private val equalsSpace = reader.isEnabled(equalsSpaceKeys)
        private val colonBefore = reader.isEnabled(colonBeforeKeys)
        private val colonAfter = reader.isEnabled(colonAfterKeys)
        private val spaceOps = reader.isEnabled(spaceOpsKeys)

        val anyGapRule = equalsNoSpace || equalsSpace || colonBefore || colonAfter || spaceOps

        fun decide(
            prev: FormatToken,
            next: FormatToken,
            gap: String,
        ): String {
            if (equalsNoSpace && (prev.text == "=" || next.text == "=")) return ""
            if (equalsSpace && (prev.text == "=" || next.text == "=")) return " "
            if (colonAfter && prev.text == ":") return " "
            if (colonBefore && next.text == ":") return " "
            if (spaceOps && (prev.kind == TokenKind.OPERATOR || next.kind == TokenKind.OPERATOR)) return " "
            return gap
        }
    }

    /**
     * Recorre tokens duros reconstruyendo el texto. El [decide] recibe (prev, next, gapOriginal)
     * y devuelve el separador a usar. Preserva el texto de los tokens y, por defecto, el gap
     * original (incluidos saltos de línea). No agrega trailing newline.
     */
    private fun rewriteGaps(
        tokens: List<FormatToken>,
        decide: (FormatToken, FormatToken, String) -> String,
    ): String {
        val sb = StringBuilder()
        var prev: FormatToken? = null
        var pendingGap = StringBuilder()
        for (token in tokens) {
            if (token.kind == TokenKind.WHITESPACE) {
                pendingGap.append(token.text)
                continue
            }
            if (prev != null) {
                sb.append(decide(prev, token, pendingGap.toString()))
            }
            sb.append(token.text)
            prev = token
            pendingGap = StringBuilder()
        }
        return sb.toString()
    }

    /** single-space: un espacio entre tokens horizontales; preserva saltos de línea entre statements. */
    private fun singleSpace(tokens: List<FormatToken>): String =
        rewriteGaps(tokens) { prev, next, gap ->
            when {
                gap.contains("\n") -> "\n"
                prev.text == "(" -> " "
                next.text == ")" -> " "
                next.text == ";" -> ""
                prev.text == ";" -> " "
                else -> " "
            }
        }

    private fun ifBrace(
        source: String,
        below: Boolean,
    ): String {
        val text = source.replace("\r\n", "\n")
        val replacement = if (below) ")\n{" else ") {"
        return Regex("""\)\s*\{""").replace(text, replacement).trimEnd('\n')
    }

    private fun lineBreakAfterStatement(source: String): String =
        splitStatements(source).joinToString("\n") { it.trim() }

    private fun printlnBreaks(
        source: String,
        n: Int,
    ): String {
        val statements = splitStatements(source).map { it.trim() }.filter { it.isNotEmpty() }
        val sb = StringBuilder()
        for ((idx, stmt) in statements.withIndex()) {
            sb.append(stmt)
            if (idx < statements.size - 1) {
                sb.append("\n")
                if (stmt.startsWith("println")) sb.append("\n".repeat(n))
            }
        }
        return sb.toString()
    }

    /** Parte el source en statements de nivel superior por ';' (conservando el ';'), respetando strings. */
    private fun splitStatements(source: String): List<String> {
        val result = mutableListOf<String>()
        val current = StringBuilder()
        var inString = false
        for (c in source.replace("\r\n", "\n")) {
            if (c == '"') inString = !inString
            current.append(c)
            if (c == ';' && !inString) {
                result.add(current.toString())
                current.clear()
            }
        }
        if (current.isNotBlank()) result.add(current.toString())
        return result
    }

    private fun indentInsideIf(
        source: String,
        indent: Int,
    ): String {
        val lines = source.replace("\r\n", "\n").split("\n")
        val out = mutableListOf<String>()
        var depth = 0
        for (line in lines) {
            val trimmed = line.trim()
            if (trimmed.startsWith("}")) depth = (depth - 1).coerceAtLeast(0)
            out.add(if (trimmed.isEmpty()) "" else " ".repeat(indent * depth) + trimmed)
            if (trimmed.endsWith("{")) depth++
        }
        return out.joinToString("\n").trimEnd('\n')
    }

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
