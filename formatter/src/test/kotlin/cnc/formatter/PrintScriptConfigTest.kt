package cnc.formatter

import cnc.ast.Assignment
import cnc.ast.BinaryExpression
import cnc.ast.Call
import cnc.ast.Declaration
import cnc.ast.Expression
import cnc.ast.Identifier
import cnc.ast.NumberLiteral
import cnc.ast.StringLiteral
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Espejo verificable de `app/config/Formatter.kt`: replica las MISMAS reglas
 * (espaciados, precedencias y formas) para verificar el output esperado de
 * PrintScript de forma ejecutable. Si esa config cambia, este espejo debe cambiar.
 */
class PrintScriptConfigTest {

    private val precedences = mapOf("+" to 1, "-" to 1, "*" to 2, "/" to 2, "**" to 3)

    private val spaceAround = FormatRule<String> { s, _ -> " $s " }
    private val spaceAfter = FormatRule<String> { s, _ -> "$s " }
    private val noSpace = FormatRule<String> { s, _ -> s }

    private val symbolRules: Map<String, FormatRule<String>> = mapOf(
        "+" to spaceAround, "-" to spaceAround, "*" to spaceAround,
        "/" to spaceAround, "**" to spaceAround,
        "=" to spaceAround, ":" to spaceAfter,
        "let" to noSpace, "const" to noSpace, ";" to noSpace
    )

    private fun formatNumber(v: Double) =
        if (v % 1.0 == 0.0) v.toLong().toString() else v.toString()

    private val expressionRules = listOf(
        ExpressionRule { e, _ -> (e as? NumberLiteral)?.let { formatNumber(it.value) } },
        ExpressionRule { e, _ -> (e as? StringLiteral)?.let { "\"${it.value}\"" } },
        ExpressionRule { e, _ -> (e as? Identifier)?.name },
        ExpressionRule { e, ctx ->
            (e as? BinaryExpression)?.let { bin ->
                val p = precedences.getValue(bin.operator)
                val l = ctx.formatOperand(bin.left, p, OperandSide.LEFT)
                val r = ctx.formatOperand(bin.right, p, OperandSide.RIGHT)
                "$l${ctx.formatSymbol(bin.operator)}$r"
            }
        }
    )

    private val declRule = StatementRule { stmt, ctx ->
        (stmt as? Declaration)?.let { decl ->
            buildString {
                append(ctx.formatSymbol(if (decl.isMutable) "let" else "const")).append(" ")
                append(decl.name)
                append(ctx.formatSymbol(":")).append(decl.type)
                decl.value?.let {
                    append(ctx.formatSymbol("=")).append(ctx.formatExpression(it))
                }
                append(ctx.formatSymbol(";"))
            }
        }
    }
    private val assignRule = StatementRule { stmt, ctx ->
        (stmt as? Assignment)?.let { assign ->
            buildString {
                append(assign.target)
                append(ctx.formatSymbol("=")).append(ctx.formatExpression(assign.value))
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

    private val formatter = Formatter(
        statementRules = listOf(declRule, assignRule, callRule),
        symbolRules = symbolRules,
        expressionRules = expressionRules,
        precedences = precedences
    )

    private fun decl(name: String, type: String, value: Expression) =
        Declaration(name = name, type = type, value = value)

    private fun assign(target: String, value: Expression) =
        Assignment(target = target, value = value)

    @Test
    fun `declaration number`() {
        assertEquals("let x: number = 42;", formatter.format(listOf(decl("x", "number", NumberLiteral(42.0)))))
    }

    @Test
    fun `declaration string`() {
        assertEquals(
            "let s: string = \"hi\";",
            formatter.format(listOf(decl("s", "string", StringLiteral("hi"))))
        )
    }

    @Test
    fun `declaration with binary and precedence parentheses`() {
        // (a + b) * 2
        val inner = BinaryExpression(Identifier("a"), "+", Identifier("b"))
        val expr = BinaryExpression(inner, "*", NumberLiteral(2.0))
        assertEquals(
            "let r: number = (a + b) * 2;",
            formatter.format(listOf(decl("r", "number", expr)))
        )
    }

    @Test
    fun `assignment`() {
        val expr = BinaryExpression(Identifier("x"), "+", NumberLiteral(1.0))
        assertEquals("x = x + 1;", formatter.format(listOf(assign("x", expr))))
    }

    @Test
    fun `call with arguments`() {
        val call = Call(function = "println", arguments = listOf(Identifier("x")))
        assertEquals("println(x);", formatter.format(listOf(call)))
    }
}
