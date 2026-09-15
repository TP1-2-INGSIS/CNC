package cnc.config

import cnc.ast.Assignment
import cnc.ast.BinaryExpression
import cnc.ast.Call
import cnc.ast.Declaration
import cnc.ast.Identifier
import cnc.ast.NumberLiteral
import cnc.ast.StringLiteral
import cnc.ast.UnaryExpression

import cnc.formatter.ExpressionRule
import cnc.formatter.FormatRule
import cnc.formatter.Formatter
import cnc.formatter.OperandSide
import cnc.formatter.StatementRule

// =============================================================================
// Configuración concreta del Formatter de PrintScript (Decisión 8)
//
// Igual que Lexer.kt / Grammar.kt / Token.kt, la config del formatter se define
// como código Kotlin y se inyecta al motor genérico :formatter. El motor no
// conoce PrintScript; todo lo específico del lenguaje vive aquí.
// =============================================================================

// -----------------------------------------------------------------------------
// Precedencias (Decisión 10) — usadas para reintroducir paréntesis por
// precedencia, ya que el AST no preserva los paréntesis originales.
// -----------------------------------------------------------------------------
val printScriptPrecedences: Map<String, Int> = mapOf(
    "+" to 1,
    "-" to 1,
    "*" to 2,
    "/" to 2,
    "**" to 3
)

// -----------------------------------------------------------------------------
// Reglas de símbolos (fase `specific`) — espaciado fino de cada símbolo/operador.
// Fuente única de verdad del espaciado, reutilizada por todas las reglas.
// -----------------------------------------------------------------------------
private val spaceAround: FormatRule<String> = FormatRule { symbol, _ -> " $symbol " }
private val spaceAfter: FormatRule<String> = FormatRule { symbol, _ -> "$symbol " }
private val noSpace: FormatRule<String> = FormatRule { symbol, _ -> symbol }

val printScriptSymbolRules: Map<String, FormatRule<String>> = mapOf(
    // operadores: un espacio a cada lado -> "a + b"
    "+" to spaceAround,
    "-" to spaceAround,
    "*" to spaceAround,
    "/" to spaceAround,
    "**" to spaceAround,
    // estructurales
    "=" to spaceAround,   // "x = 5"
    ":" to spaceAfter,    // "x: number"  (sin espacio antes, uno después)
    "let" to noSpace,     // el espacio tras `let` lo pone la statement rule
    "const" to noSpace,   // idem para `const`
    ";" to noSpace        // ";" pegado, sin espacio
)

// -----------------------------------------------------------------------------
// Reglas de expresiones (Decisión 9) — self-dispatch: cada regla se autoevalúa
// con `as?` (safe cast: devuelve null si el nodo no es de ese tipo, en cuyo caso
// el motor prueba la siguiente regla).
// -----------------------------------------------------------------------------
private val numberRule = ExpressionRule { expr, _ ->
    // `as?`: null si no es NumberLiteral -> esta regla no aplica.
    (expr as? NumberLiteral)?.let { formatNumber(it.value) }
}

private val stringRule = ExpressionRule { expr, _ ->
    (expr as? StringLiteral)?.let { "\"${it.value}\"" }
}

private val identifierRule = ExpressionRule { expr, _ ->
    (expr as? Identifier)?.name
}

private val binaryRule = ExpressionRule { expr, ctx ->
    (expr as? BinaryExpression)?.let { bin ->
        val precedence = printScriptPrecedences[bin.operator]
            ?: error("No precedence for operator '${bin.operator}'")
        // formatOperand reintroduce paréntesis si el hijo lo requiere (Decisión 10).
        val left = ctx.formatOperand(bin.left, precedence, OperandSide.LEFT)
        val right = ctx.formatOperand(bin.right, precedence, OperandSide.RIGHT)
        "$left${ctx.formatSymbol(bin.operator)}$right"
    }
}

private val unaryRule = ExpressionRule { expr, ctx ->
    (expr as? UnaryExpression)?.let { unary ->
        val precedence = printScriptPrecedences[unary.operator] ?: Int.MAX_VALUE
        val operand = ctx.formatOperand(unary.operand, precedence, OperandSide.RIGHT)
        "${unary.operator}$operand"
    }
}

private val booleanRule = ExpressionRule { expr, _ ->
    (expr as? cnc.ast.BooleanLiteral)?.let { it.value.toString() }
}

private val callExpressionRule = ExpressionRule { expr, ctx ->
    (expr as? cnc.ast.CallExpression)?.let { call ->
        val args = call.arguments.joinToString(", ") { ctx.formatExpression(it) }
        "${call.function}($args)"
    }
}

val printScriptExpressionRules: List<ExpressionRule> = listOf(
    numberRule,
    stringRule,
    booleanRule,
    identifierRule,
    binaryRule,
    unaryRule,
    callExpressionRule
)

/** `5.0` -> `"5"`, `3.14` -> `"3.14"` (los enteros se muestran sin decimal). */
private fun formatNumber(value: Double): String =
    if (value % 1.0 == 0.0) value.toLong().toString() else value.toString()

// -----------------------------------------------------------------------------
// Reglas de statements (fase `general`) — self-dispatch sobre el AST tipado.
// Reconstruyen la forma del statement manualmente (Decisión 6), delegando cada
// símbolo y expresión en el contexto.
// -----------------------------------------------------------------------------

// "let name: type = value;" / "const name: type = value;" (sin `= value` si no hay init).
private val variableDeclarationRule = StatementRule { stmt, ctx ->
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

// "target = value;"
private val variableAssignmentRule = StatementRule { stmt, ctx ->
    (stmt as? Assignment)?.let { assign ->
        buildString {
            append(assign.target)
            append(ctx.formatSymbol("="))
            append(ctx.formatExpression(assign.value))
            append(ctx.formatSymbol(";"))
        }
    }
}

// "function(arg1, arg2);"
private val callRule = StatementRule { stmt, ctx ->
    (stmt as? Call)?.let { call ->
        val args = call.arguments.joinToString(", ") { ctx.formatExpression(it) }
        "${call.function}($args)${ctx.formatSymbol(";")}"
    }
}

// "{\n    stmt1;\n    stmt2;\n}"
private val blockStatementRule = StatementRule { stmt, ctx ->
    (stmt as? cnc.ast.BlockStatement)?.let { block ->
        if (block.statements.isEmpty()) {
            "{}"
        } else {
            val stmts = block.statements.mapNotNull { inner ->
                printScriptStatementRules.firstNotNullOfOrNull { rule -> rule.tryFormat(inner, ctx) }
            }
            "{\n" + stmts.joinToString("\n") { "    " + it.replace("\n", "\n    ") } + "\n}"
        }
    }
}

// "if (condition) {\n    ...\n} else {\n    ...\n}"
private val ifStatementRule = StatementRule { stmt, ctx ->
    (stmt as? cnc.ast.IfStatement)?.let { ifStmt ->
        val cond = ctx.formatExpression(ifStmt.condition)
        val thenBlock = blockStatementRule.tryFormat(ifStmt.thenBlock, ctx)
        val elseB = ifStmt.elseBlock
        if (elseB != null) {
            val elseBlock = blockStatementRule.tryFormat(elseB, ctx)
            "if ($cond) $thenBlock else $elseBlock"
        } else {
            "if ($cond) $thenBlock"
        }
    }
}

val printScriptStatementRules: List<StatementRule> = listOf(
    variableDeclarationRule,
    variableAssignmentRule,
    callRule,
    blockStatementRule,
    ifStatementRule
)

// -----------------------------------------------------------------------------
// Formatter de PrintScript, listo para inyectar/usar.
// -----------------------------------------------------------------------------
val printScriptFormatter = Formatter(
    statementRules = printScriptStatementRules,
    symbolRules = printScriptSymbolRules,
    expressionRules = printScriptExpressionRules,
    precedences = printScriptPrecedences
)
