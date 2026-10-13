package cnc.formatter

import cnc.ast.Assignment
import cnc.ast.BinaryExpression
import cnc.ast.Call
import cnc.ast.Declaration
import cnc.ast.Identifier
import cnc.ast.NumberLiteral
import cnc.ast.StringLiteral
import cnc.ast.UnaryExpression

object StandardFormatRules {

    val defaultPrecedences: Map<String, Int> = mapOf(
        "+" to 1,
        "-" to 1,
        "*" to 2,
        "/" to 2,
        "**" to 3
    )

    private val spaceAround: FormatRule<String> = FormatRule { symbol, _ -> " $symbol " }
    private val spaceAfter: FormatRule<String> = FormatRule { symbol, _ -> "$symbol " }
    private val noSpace: FormatRule<String> = FormatRule { symbol, _ -> symbol }

    val defaultSymbolRules: Map<String, FormatRule<String>> = mapOf(
        "+" to spaceAround,
        "-" to spaceAround,
        "*" to spaceAround,
        "/" to spaceAround,
        "**" to spaceAround,
        "=" to spaceAround,
        ":" to spaceAfter,
        "let" to noSpace,
        "const" to noSpace,
        ";" to noSpace
    )

    private fun formatNumber(value: Double): String =
        if (value % 1.0 == 0.0) value.toLong().toString() else value.toString()

    private val numberRule = ExpressionRule { expr, _ ->
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
            val precedence = defaultPrecedences[bin.operator]
                ?: error("No precedence for operator '${bin.operator}'")
            val left = ctx.formatOperand(bin.left, precedence, OperandSide.LEFT)
            val right = ctx.formatOperand(bin.right, precedence, OperandSide.RIGHT)
            "$left${ctx.formatSymbol(bin.operator)}$right"
        }
    }

    private val unaryRule = ExpressionRule { expr, ctx ->
        (expr as? UnaryExpression)?.let { unary ->
            val precedence = defaultPrecedences[unary.operator] ?: Int.MAX_VALUE
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

    val defaultExpressionRules: List<ExpressionRule> = listOf(
        numberRule,
        stringRule,
        booleanRule,
        identifierRule,
        binaryRule,
        unaryRule,
        callExpressionRule
    )

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

    private val callRule = StatementRule { stmt, ctx ->
        (stmt as? Call)?.let { call ->
            val args = call.arguments.joinToString(", ") { ctx.formatExpression(it) }
            "${call.function}($args)${ctx.formatSymbol(";")}"
        }
    }

    private val blockStatementRule = StatementRule { stmt, ctx ->
        (stmt as? cnc.ast.BlockStatement)?.let { block ->
            if (block.statements.isEmpty()) {
                "{}"
            } else {
                val stmts = block.statements.map { inner -> ctx.formatStatement(inner) }
                "{\n" + stmts.joinToString("\n") { "    " + it.replace("\n", "\n    ") } + "\n}"
            }
        }
    }

    private val ifStatementRule = StatementRule { stmt, ctx ->
        (stmt as? cnc.ast.IfStatement)?.let { ifStmt ->
            val cond = ctx.formatExpression(ifStmt.condition)
            val thenBlock = ctx.formatStatement(ifStmt.thenBlock)
            val elseB = ifStmt.elseBlock
            if (elseB != null) {
                val elseBlock = ctx.formatStatement(elseB)
                "if ($cond) $thenBlock else $elseBlock"
            } else {
                "if ($cond) $thenBlock"
            }
        }
    }

    val defaultStatementRules: List<StatementRule> = listOf(
        variableDeclarationRule,
        variableAssignmentRule,
        callRule,
        blockStatementRule,
        ifStatementRule
    )

    fun defaultFormatter(): Formatter = Formatter(
        statementRules = defaultStatementRules,
        symbolRules = defaultSymbolRules,
        expressionRules = defaultExpressionRules,
        precedences = defaultPrecedences
    )
}
