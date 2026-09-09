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
import kotlin.test.assertFailsWith

class FormatterTest {

    // --- Reglas fixture (simulan lo que vivirá en app/config/) ---------------

    private val numberRule = ExpressionRule { expr, _ ->
        (expr as? NumberLiteral)?.let {
            if (it.value % 1.0 == 0.0) it.value.toLong().toString() else it.value.toString()
        }
    }
    private val stringRule = ExpressionRule { expr, _ ->
        (expr as? StringLiteral)?.let { "\"${it.value}\"" }
    }
    private val identifierRule = ExpressionRule { expr, _ ->
        (expr as? Identifier)?.name
    }
    private val binaryRule = ExpressionRule { expr, ctx ->
        (expr as? BinaryExpression)?.let { bin ->
            val prec = precedences.getValue(bin.operator)
            val left = ctx.formatOperand(bin.left, prec, OperandSide.LEFT)
            val right = ctx.formatOperand(bin.right, prec, OperandSide.RIGHT)
            "$left${ctx.formatSymbol(bin.operator)}$right"
        }
    }

    private val expressionRules = listOf(numberRule, stringRule, identifierRule, binaryRule)

    // Espaciado: operadores con un espacio a cada lado; símbolos estructurales
    // configurados aparte.
    private val symbolRules: Map<String, FormatRule<String>> = mapOf(
        "+" to FormatRule { s, _ -> " $s " },
        "*" to FormatRule { s, _ -> " $s " },
        "let" to FormatRule { s, _ -> s },
        ":" to FormatRule { s, _ -> "$s " },
        "=" to FormatRule { s, _ -> " $s " },
        ";" to FormatRule { s, _ -> s }
    )

    private val precedences = mapOf("+" to 1, "*" to 2)

    // Regla de statement: VariableDeclaration -> "let name: type = value;"
    private val declRule = FormatRule<GenericStatement> { stmt, ctx ->
        val f = stmt.fields
        buildString {
            append(ctx.formatSymbol("let")).append(" ")
            append(f.text("name"))
            append(ctx.formatSymbol(":")).append(f.text("type"))
            append(ctx.formatSymbol("="))
            append(ctx.formatExpression(f.expression("value")))
            append(ctx.formatSymbol(";"))
        }
    }

    private val statementRules = mapOf("VariableDeclaration" to declRule)

    private fun formatter() = Formatter(statementRules, symbolRules, expressionRules, precedences)

    private val declDef = StatementDef(
        tag = "VariableDeclaration",
        fields = emptyMap(),
        semanticCheck = { _, _ -> Success("ok", Unit) as Result<Unit> }
    )

    private fun decl(name: String, type: String, value: Expression) =
        GenericStatement(
            def = declDef,
            fields = Fields(mapOf("name" to name, "type" to type, "value" to value))
        )

    // --- Tests ---------------------------------------------------------------

    @Test
    fun `formats a simple declaration with a number literal`() {
        val ast = listOf(decl("x", "number", NumberLiteral(5.0)))
        assertEquals("let x: number = 5;", formatter().format(ast))
    }

    @Test
    fun `formats a binary expression with operator spacing`() {
        val expr = BinaryExpression(Identifier("x"), "+", NumberLiteral(3.0))
        val ast = listOf(decl("y", "number", expr))
        assertEquals("let y: number = x + 3;", formatter().format(ast))
    }

    @Test
    fun `reintroduces parentheses when child precedence is lower than parent`() {
        // (1 + 2) * 3  →  árbol: Binary(Binary(1,+,2), *, 3)
        val inner = BinaryExpression(NumberLiteral(1.0), "+", NumberLiteral(2.0))
        val expr = BinaryExpression(inner, "*", NumberLiteral(3.0))
        val ast = listOf(decl("z", "number", expr))
        assertEquals("let z: number = (1 + 2) * 3;", formatter().format(ast))
    }

    @Test
    fun `omits parentheses when child precedence is higher than parent`() {
        // 1 + 2 * 3  →  árbol: Binary(1, +, Binary(2,*,3)) — no requiere paréntesis
        val inner = BinaryExpression(NumberLiteral(2.0), "*", NumberLiteral(3.0))
        val expr = BinaryExpression(NumberLiteral(1.0), "+", inner)
        val ast = listOf(decl("z", "number", expr))
        assertEquals("let z: number = 1 + 2 * 3;", formatter().format(ast))
    }

    @Test
    fun `formats multiple statements separated by newline`() {
        val ast = listOf(
            decl("a", "number", NumberLiteral(1.0)),
            decl("b", "string", StringLiteral("hi"))
        )
        assertEquals("let a: number = 1;\nlet b: string = \"hi\";", formatter().format(ast))
    }

    @Test
    fun `fails when no statement rule is registered for a tag`() {
        val unknownDef = StatementDef("Unknown", emptyMap()) { _, _ -> Success("ok", Unit) as Result<Unit> }
        val ast = listOf(GenericStatement(unknownDef, Fields(emptyMap())))
        assertFailsWith<IllegalStateException> { formatter().format(ast) }
    }

    @Test
    fun `fails when no expression rule applies`() {
        // Formatter sin reglas de expresión, con un value que requiere una.
        val bare = Formatter(statementRules, symbolRules, emptyList(), precedences)
        val ast = listOf(decl("x", "number", NumberLiteral(5.0)))
        assertFailsWith<IllegalStateException> { bare.format(ast) }
    }
}
