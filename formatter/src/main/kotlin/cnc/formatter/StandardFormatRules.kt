package cnc.formatter

import cnc.ast.Assignment
import cnc.ast.BinaryExpression
import cnc.ast.BlockStatement
import cnc.ast.BooleanLiteral
import cnc.ast.Call
import cnc.ast.CallExpression
import cnc.ast.Declaration
import cnc.ast.Identifier
import cnc.ast.IfStatement
import cnc.ast.NumberLiteral
import cnc.ast.StringLiteral
import cnc.ast.UnaryExpression

/** Catálogo canónico de reglas del formatter, definidas una sola vez y parametrizadas por [FormatConfig]. */
object StandardFormatRules {
    val defaultPrecedences: Map<String, Int> =
        mapOf(
            "+" to 1,
            "-" to 1,
            "*" to 2,
            "/" to 2,
            "**" to 3,
        )

    // Reglas de expresión, independientes de la config.
    private fun formatNumber(value: Double): String =
        if (value % 1.0 == 0.0) value.toLong().toString() else value.toString()

    private val numberRule =
        ExpressionRule { expr, _ ->
            (expr as? NumberLiteral)?.let { formatNumber(it.value) }
        }

    private val stringRule =
        ExpressionRule { expr, _ ->
            (expr as? StringLiteral)?.let { "\"${it.value}\"" }
        }

    private val identifierRule =
        ExpressionRule { expr, _ ->
            (expr as? Identifier)?.name
        }

    private val binaryRule =
        ExpressionRule { expr, ctx ->
            (expr as? BinaryExpression)?.let { bin ->
                val precedence =
                    defaultPrecedences[bin.operator]
                        ?: error("No precedence for operator '${bin.operator}'")
                val left = ctx.formatOperand(bin.left, precedence, OperandSide.LEFT)
                val right = ctx.formatOperand(bin.right, precedence, OperandSide.RIGHT)
                "$left${ctx.formatSymbol(bin.operator)}$right"
            }
        }

    private val unaryRule =
        ExpressionRule { expr, ctx ->
            (expr as? UnaryExpression)?.let { unary ->
                val precedence = defaultPrecedences[unary.operator] ?: Int.MAX_VALUE
                val operand = ctx.formatOperand(unary.operand, precedence, OperandSide.RIGHT)
                "${unary.operator}$operand"
            }
        }

    private val booleanRule =
        ExpressionRule { expr, _ ->
            (expr as? BooleanLiteral)?.let { it.value.toString() }
        }

    private val callExpressionRule =
        ExpressionRule { expr, ctx ->
            (expr as? CallExpression)?.let { call ->
                val args = call.arguments.joinToString(", ") { ctx.formatExpression(it) }
                "${call.function}($args)"
            }
        }

    val defaultExpressionRules: List<ExpressionRule> =
        listOf(
            numberRule,
            stringRule,
            booleanRule,
            identifierRule,
            binaryRule,
            unaryRule,
            callExpressionRule,
        )

    // Reglas de símbolo, dependen de la config.
    private fun symbolRulesFor(config: FormatConfig): Map<String, FormatRule<String>> {
        val around: FormatRule<String> = FormatRule { s, _ -> Spacing.AROUND.apply(s) }
        val equalsRule: FormatRule<String> = FormatRule { s, _ -> config.equalsSpacing.apply(s) }
        val colonRule: FormatRule<String> = FormatRule { s, _ -> config.colonSpacing.apply(s) }
        val noSpace: FormatRule<String> = FormatRule { s, _ -> Spacing.NONE.apply(s) }
        return mapOf(
            "+" to around,
            "-" to around,
            "*" to around,
            "/" to around,
            "**" to around,
            "=" to equalsRule,
            ":" to colonRule,
            "let" to noSpace,
            "const" to noSpace,
            ";" to noSpace,
        )
    }

    // Reglas de statement, dependen de la config.
    private fun statementRulesFor(config: FormatConfig): List<StatementRule> {
        val variableDeclarationRule =
            StatementRule { stmt, ctx ->
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

        val variableAssignmentRule =
            StatementRule { stmt, ctx ->
                (stmt as? Assignment)?.let { assign ->
                    buildString {
                        append(assign.target)
                        append(ctx.formatSymbol("="))
                        append(ctx.formatExpression(assign.value))
                        append(ctx.formatSymbol(";"))
                    }
                }
            }

        val callRule =
            StatementRule { stmt, ctx ->
                (stmt as? Call)?.let { call ->
                    val args = call.arguments.joinToString(", ") { ctx.formatExpression(it) }
                    val base =
                        if (config.singleSpaceSeparation) {
                            "${call.function} ( $args )${ctx.formatSymbol(";")}"
                        } else {
                            "${call.function}($args)${ctx.formatSymbol(";")}"
                        }
                    if (call.function == "println" && config.lineBreaksAfterPrintln > 0) {
                        base + "\n".repeat(config.lineBreaksAfterPrintln)
                    } else {
                        base
                    }
                }
            }

        val blockStatementRule =
            StatementRule { stmt, ctx ->
                (stmt as? BlockStatement)?.let { block ->
                    if (block.statements.isEmpty()) {
                        "{}"
                    } else {
                        val indent = " ".repeat(config.indentInsideIf)
                        val stmts = block.statements.map { inner -> ctx.formatStatement(inner) }
                        "{\n" + stmts.joinToString("\n") { indent + it.replace("\n", "\n$indent") } + "\n}"
                    }
                }
            }

        val ifStatementRule =
            StatementRule { stmt, ctx ->
                (stmt as? IfStatement)?.let { ifStmt ->
                    val cond = ctx.formatExpression(ifStmt.condition)
                    val thenBlock = ctx.formatStatement(ifStmt.thenBlock)
                    val sep = if (config.ifBraceBelowLine) "\n" else " "
                    val elseB = ifStmt.elseBlock
                    if (elseB != null) {
                        val elseBlock = ctx.formatStatement(elseB)
                        "if ($cond)$sep$thenBlock else $elseBlock"
                    } else {
                        "if ($cond)$sep$thenBlock"
                    }
                }
            }

        return listOf(
            variableDeclarationRule,
            variableAssignmentRule,
            callRule,
            blockStatementRule,
            ifStatementRule,
        )
    }

    /** Reglas de símbolo por defecto (config [FormatConfig.DEFAULT]). */
    val defaultSymbolRules: Map<String, FormatRule<String>>
        get() = symbolRulesFor(FormatConfig.DEFAULT)

    /** Reglas de statement por defecto (config [FormatConfig.DEFAULT]). */
    val defaultStatementRules: List<StatementRule>
        get() = statementRulesFor(FormatConfig.DEFAULT)

    /** Construye un [Formatter] para la [config] dada. */
    fun formatterFor(config: FormatConfig): Formatter =
        Formatter(
            statementRules = statementRulesFor(config),
            symbolRules = symbolRulesFor(config),
            expressionRules = defaultExpressionRules,
            precedences = defaultPrecedences,
        )

    fun defaultFormatter(): Formatter = formatterFor(FormatConfig.DEFAULT)
}
