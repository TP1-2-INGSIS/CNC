package cnc.config

import cnc.ast.Assignment
import cnc.ast.BlockStatement
import cnc.ast.Call
import cnc.ast.Declaration
import cnc.ast.IfStatement
import cnc.common.ErrorType
import cnc.common.Failure
import cnc.common.Result
import cnc.common.Success
import cnc.formatter.FormatRule
import cnc.formatter.Formatter
import cnc.formatter.StatementRule
import com.google.gson.Gson
import com.google.gson.JsonObject

/**
 * Fábrica de [Formatter] que lee la configuración JSON (con las opciones de formato del TCK)
 * y construye una instancia con las [StatementRule] y [FormatRule] correspondientes.
 */
object FormatterFactory {

    private val equalsSpaceAroundKeys = listOf("enforce-spacing-around-equals", "space-around-equals")
    private val equalsNoSpaceAroundKeys = listOf("enforce-no-spacing-around-equals", "no-space-around-equals")
    private val colonBeforeKeys = listOf("enforce-spacing-before-colon-in-declaration", "space-before-colon")
    private val colonAfterKeys = listOf("enforce-spacing-after-colon-in-declaration", "space-after-colon")
    private val singleSpaceSeparationKeys = listOf("mandatory-single-space-separation", "single-space-separation")
    private val printlnLineBreaksKeys = listOf("line-breaks-after-println", "line-break-after-println")
    private val lineBreakAfterStatementKeys = listOf("mandatory-line-break-after-statement", "line-break-after-statement")
    private val spaceOpsKeys = listOf("mandatory-space-surrounding-operations", "enforce-spacing-around-operations")
    private val ifBraceBelowLineKeys = listOf("if-brace-below-line")
    private val ifBraceSameLineKeys = listOf("if-brace-same-line")
    private val indentInsideIfKeys = listOf("indent-inside-if")

    private val spaceAround: FormatRule<String> = FormatRule { s, _ -> " $s " }
    private val spaceAfter: FormatRule<String> = FormatRule { s, _ -> "$s " }
    private val noSpace: FormatRule<String> = FormatRule { s, _ -> s }

    fun build(jsonString: String?): Result<Formatter> {
        if (jsonString.isNullOrBlank()) {
            return Success("ok", printScriptFormatter)
        }

        val root = runCatching {
            Gson().fromJson(jsonString, JsonObject::class.java)
        }.getOrNull() ?: return Failure("Invalid formatter JSON configuration", ErrorType.CLI)

        val noSpaceAroundEquals = isRuleEnabled(root, equalsNoSpaceAroundKeys)
        val spaceAroundEquals = isRuleEnabled(root, equalsSpaceAroundKeys)
        val spaceBeforeColon = isRuleEnabled(root, colonBeforeKeys)
        val spaceAfterColon = isRuleEnabled(root, colonAfterKeys)
        val singleSpace = isRuleEnabled(root, singleSpaceSeparationKeys)
        val lineBreaksAfterPrintln = readIntProperty(root, printlnLineBreaksKeys)
        val ifBraceBelowLine = isRuleEnabled(root, ifBraceBelowLineKeys)
        val indentInsideIf = readIntProperty(root, indentInsideIfKeys) ?: 2

        // 1. Símbolos
        val equalsRule = when {
            noSpaceAroundEquals -> noSpace
            spaceAroundEquals || singleSpace -> spaceAround
            else -> spaceAround
        }

        val colonRule = when {
            singleSpace -> spaceAround
            spaceBeforeColon && spaceAfterColon -> spaceAround
            spaceBeforeColon -> FormatRule<String> { s, _ -> " $s" }
            spaceAfterColon -> spaceAfter
            else -> spaceAfter
        }

        val symbolRules: Map<String, FormatRule<String>> = mapOf(
            "+" to spaceAround,
            "-" to spaceAround,
            "*" to spaceAround,
            "/" to spaceAround,
            "**" to spaceAround,
            "=" to equalsRule,
            ":" to colonRule,
            "let" to noSpace,
            "const" to noSpace,
            ";" to noSpace
        )

        // 2. Statements
        val variableDeclarationRule = StatementRule { stmt, ctx ->
            (stmt as? Declaration)?.let { decl ->
                buildString {
                    append(ctx.formatSymbol(if (decl.isMutable) "let" else "const")).append(" ")
                    append(decl.name)
                    append(ctx.formatSymbol(":"))
                    append(decl.type)
                    decl.value?.let {
                        append(ctx.formatSymbol("="))
                        append(ctx.formatExpression(it))
                    }
                    append(ctx.formatSymbol(";"))
                }
            }
        }

        val variableAssignmentRule = StatementRule { stmt, ctx ->
            (stmt as? Assignment)?.let { assign ->
                buildString {
                    append(assign.target)
                    append(ctx.formatSymbol("="))
                    append(ctx.formatExpression(assign.value))
                    append(ctx.formatSymbol(";"))
                }
            }
        }

        val callRule = StatementRule { stmt, ctx ->
            (stmt as? Call)?.let { call ->
                val args = call.arguments.joinToString(", ") { ctx.formatExpression(it) }
                val base = if (singleSpace) {
                    "${call.function} ( $args )${ctx.formatSymbol(";")}"
                } else {
                    "${call.function}($args)${ctx.formatSymbol(";")}"
                }
                if (call.function == "println" && lineBreaksAfterPrintln != null && lineBreaksAfterPrintln > 0) {
                    base + "\n".repeat(lineBreaksAfterPrintln)
                } else {
                    base
                }
            }
        }

        val blockStatementRule = StatementRule { stmt, ctx ->
            (stmt as? BlockStatement)?.let { block ->
                if (block.statements.isEmpty()) {
                    "{}"
                } else {
                    val indent = " ".repeat(indentInsideIf)
                    val stmts = block.statements.map { inner -> ctx.formatStatement(inner) }
                    "{\n" + stmts.joinToString("\n") { indent + it.replace("\n", "\n$indent") } + "\n}"
                }
            }
        }

        val ifStatementRule = StatementRule { stmt, ctx ->
            (stmt as? IfStatement)?.let { ifStmt ->
                val cond = ctx.formatExpression(ifStmt.condition)
                val thenBlock = ctx.formatStatement(ifStmt.thenBlock)
                val sep = if (ifBraceBelowLine) "\n" else " "
                val elseB = ifStmt.elseBlock
                if (elseB != null) {
                    val elseBlock = ctx.formatStatement(elseB)
                    "if ($cond)$sep$thenBlock else $elseBlock"
                } else {
                    "if ($cond)$sep$thenBlock"
                }
            }
        }

        val statementRules = listOf(
            variableDeclarationRule,
            variableAssignmentRule,
            callRule,
            blockStatementRule,
            ifStatementRule
        )

        val formatter = Formatter(
            statementRules = statementRules,
            symbolRules = symbolRules,
            expressionRules = printScriptExpressionRules,
            precedences = printScriptPrecedences
        )

        return Success("ok", formatter)
    }

    fun create(jsonString: String? = null): Formatter {
        return when (val result = build(jsonString)) {
            is Success -> result.data
            is Failure -> printScriptFormatter
        }
    }

    private fun readIntProperty(json: JsonObject, candidateKeys: List<String>): Int? {
        val matchingKey = candidateKeys.firstOrNull { json.has(it) && !json.get(it).isJsonNull } ?: return null
        return runCatching { json.get(matchingKey).asInt }.getOrNull()
    }

    private fun isRuleEnabled(json: JsonObject, candidateKeys: List<String>): Boolean {
        val matchingKey = candidateKeys.firstOrNull { json.has(it) && !json.get(it).isJsonNull } ?: return false
        return runCatching { json.get(matchingKey).asBoolean }.getOrDefault(false)
    }

    fun formatSource(rawSource: String, configJson: String?): String {
        if (configJson.isNullOrBlank()) {
            return rawSource
        }

        val root = runCatching {
            Gson().fromJson(configJson, JsonObject::class.java)
        }.getOrNull() ?: return rawSource

        var text = rawSource.replace("\r\n", "\n")

        // 1. mandatory-line-break-after-statement
        if (isRuleEnabled(root, lineBreakAfterStatementKeys)) {
            text = text.replace(Regex(";[ \\t]*(?=\\S)"), ";\n")
        }

        // 2. enforce-no-spacing-around-equals / enforce-spacing-around-equals
        val noSpaceAroundEquals = isRuleEnabled(root, equalsNoSpaceAroundKeys)
        val spaceAroundEquals = isRuleEnabled(root, equalsSpaceAroundKeys)
        if (noSpaceAroundEquals) {
            text = text.replace(Regex("[ \\t]*=[ \\t]*"), "=")
        } else if (spaceAroundEquals) {
            text = text.replace(Regex("[ \\t]*=[ \\t]*"), " = ")
        }

        // 3. enforce-spacing-before-colon-in-declaration
        val spaceBeforeColon = isRuleEnabled(root, colonBeforeKeys)
        if (spaceBeforeColon) {
            text = text.replace(Regex("""\b(let|const)\s+([a-zA-Z_]\w*)\s*:""")) { match ->
                "${match.groupValues[1]} ${match.groupValues[2]} :"
            }
        }

        // 4. enforce-spacing-after-colon-in-declaration
        val spaceAfterColon = isRuleEnabled(root, colonAfterKeys)
        if (spaceAfterColon) {
            text = text.replace(Regex("""\b(let|const)\s+([a-zA-Z_]\w*(?:\s*)):\s*([a-zA-Z_]\w*)""")) { match ->
                "${match.groupValues[1]} ${match.groupValues[2]}: ${match.groupValues[3]}"
            }
        }

        // 5. mandatory-space-surrounding-operations
        val spaceOps = isRuleEnabled(root, spaceOpsKeys)
        if (spaceOps) {
            text = text.replace(Regex("""(?<=[a-zA-Z0-9_\)])\s*([+\-*/])\s*(?=[a-zA-Z0-9_\(])"""), " $1 ")
        }

        // 6. mandatory-single-space-separation
        val singleSpace = isRuleEnabled(root, singleSpaceSeparationKeys)
        if (singleSpace) {
            text = text.replace(Regex("""\b(let|const)\s+([a-zA-Z_]\w*)\s*:\s*([a-zA-Z_]\w*)\s*=""")) { m ->
                "${m.groupValues[1]} ${m.groupValues[2]} : ${m.groupValues[3]} ="
            }
            text = text.replace(Regex("""\b(let|const)\s+([a-zA-Z_]\w*)\s*:\s*([a-zA-Z_]\w*)""")) { m ->
                "${m.groupValues[1]} ${m.groupValues[2]} : ${m.groupValues[3]}"
            }
            text = text.replace(Regex("""[ \t]*=[ \t]*"""), " = ")
            text = text.replace(Regex("""\b([a-zA-Z_]\w*)\s*\(\s*(.*?)\s*\)""")) { m ->
                "${m.groupValues[1]} ( ${m.groupValues[2]} )"
            }
        }

        // 7. if-brace-below-line / if-brace-same-line
        val ifBraceBelowLine = isRuleEnabled(root, ifBraceBelowLineKeys)
        val ifBraceSameLine = isRuleEnabled(root, ifBraceSameLineKeys)
        if (ifBraceBelowLine) {
            text = text.replace(Regex("""\bif\s*\((.*?)\)[ \t]*\{"""), "if ($1)\n{")
        } else if (ifBraceSameLine) {
            text = text.replace(Regex("""\bif\s*\((.*?)\)\s*\n\s*\{"""), "if ($1) {")
        }

        // 8. indent-inside-if
        val indentInsideIf = readIntProperty(root, indentInsideIfKeys)
        if (indentInsideIf != null) {
            val indentStr = " ".repeat(indentInsideIf)
            val lines = text.lines()
            val formattedLines = mutableListOf<String>()
            var depth = 0
            for (line in lines) {
                val trimmed = line.trim()
                if (trimmed.startsWith("}")) {
                    depth = (depth - 1).coerceAtLeast(0)
                }
                val currentIndent = indentStr.repeat(depth)
                if (trimmed.isNotEmpty()) {
                    formattedLines.add(currentIndent + trimmed)
                } else {
                    formattedLines.add("")
                }
                if (trimmed.endsWith("{")) {
                    depth++
                }
            }
            text = formattedLines.joinToString("\n")
        }

        // 9. line-breaks-after-println
        val lineBreaksAfterPrintln = readIntProperty(root, printlnLineBreaksKeys)
        if (lineBreaksAfterPrintln != null) {
            text = text.replace(Regex("""(println\s*\([^)]*\)\s*;)[ \t]*(\n\s*)*""")) { match ->
                val stmt = match.groupValues[1]
                val remaining = text.substring(match.range.last + 1).trim()
                if (remaining.isEmpty()) {
                    stmt
                } else {
                    stmt + "\n".repeat(1 + lineBreaksAfterPrintln)
                }
            }
        }

        return text
    }
}
