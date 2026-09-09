package cnc.formatter

import cnc.ast.BinaryExpression
import cnc.ast.Expression
import cnc.ast.Fields
import cnc.ast.GenericStatement
import cnc.ast.Identifier
import cnc.ast.NumberLiteral
import cnc.ast.StatementDef
import cnc.ast.StringLiteral
import cnc.common.Result
import cnc.common.Success
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Espejo verificable de `app/config/Formatter.kt`.
 *
 * La config real vive en `:app`, que hoy no compila por causas ajenas al
 * formatter (`:interpreter` desactualizado, ver plan). Para poder verificar de
 * forma EJECUTABLE que las reglas de PrintScript producen el output esperado,
 * se replican aquí las MISMAS reglas (espaciados, precedencias y formas) que en
 * `app/config/Formatter.kt`. Si esa config cambia, este espejo debe cambiar.
 */
class PrintScriptConfigTest {

    private val precedences = mapOf("+" to 1, "-" to 1, "*" to 2, "/" to 2, "**" to 3)

    private val spaceAround = FormatRule<String> { s, _ -> " $s " }
    private val spaceAfter = FormatRule<String> { s, _ -> "$s " }
    private val noSpace = FormatRule<String> { s, _ -> s }

    private val symbolRules: Map<String, FormatRule<String>> = mapOf(
        "+" to spaceAround, "-" to spaceAround, "*" to spaceAround,
        "/" to spaceAround, "**" to spaceAround,
        "=" to spaceAround, ":" to spaceAfter, "let" to noSpace, ";" to noSpace
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

    private val declRule = FormatRule<GenericStatement> { stmt, ctx ->
        val f = stmt.fields
        buildString {
            append(ctx.formatSymbol("let")).append(" ")
            append(f.text("name"))
            append(ctx.formatSymbol(":")).append(f.text("type"))
            append(ctx.formatSymbol("=")).append(ctx.formatExpression(f.expression("value")))
            append(ctx.formatSymbol(";"))
        }
    }
    private val assignRule = FormatRule<GenericStatement> { stmt, ctx ->
        val f = stmt.fields
        buildString {
            append(f.text("target"))
            append(ctx.formatSymbol("=")).append(ctx.formatExpression(f.expression("value")))
            append(ctx.formatSymbol(";"))
        }
    }

    private val formatter = Formatter(
        statementRules = mapOf(
            "VariableDeclaration" to declRule,
            "VariableAssignment" to assignRule
        ),
        symbolRules = symbolRules,
        expressionRules = expressionRules,
        precedences = precedences
    )

    private val declDef = StatementDef("VariableDeclaration", emptyMap()) { _, _ ->
        Success("ok", Unit) as Result<Unit>
    }
    private val assignDef = StatementDef("VariableAssignment", emptyMap()) { _, _ ->
        Success("ok", Unit) as Result<Unit>
    }

    private fun decl(name: String, type: String, value: Expression) =
        GenericStatement(declDef, Fields(mapOf("name" to name, "type" to type, "value" to value)))

    private fun assign(target: String, value: Expression) =
        GenericStatement(assignDef, Fields(mapOf("target" to target, "value" to value)))

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
}
