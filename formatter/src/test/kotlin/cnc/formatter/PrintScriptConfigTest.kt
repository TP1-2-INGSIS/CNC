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

class PrintScriptConfigTest {
    private val formatter = StandardFormatRules.defaultFormatter()

    private fun decl(
        name: String,
        type: String,
        value: Expression,
    ) = Declaration(name = name, type = type, value = value)

    private fun assign(
        target: String,
        value: Expression,
    ) = Assignment(target = target, value = value)

    @Test
    fun `declaration number`() {
        assertEquals("let x: number = 42;", formatter.format(listOf(decl("x", "number", NumberLiteral(42.0)))))
    }

    @Test
    fun `declaration string`() {
        assertEquals(
            "let s: string = \"hi\";",
            formatter.format(listOf(decl("s", "string", StringLiteral("hi")))),
        )
    }

    @Test
    fun `declaration with binary and precedence parentheses`() {
        // (a + b) * 2
        val inner = BinaryExpression(Identifier("a"), "+", Identifier("b"))
        val expr = BinaryExpression(inner, "*", NumberLiteral(2.0))
        assertEquals(
            "let r: number = (a + b) * 2;",
            formatter.format(listOf(decl("r", "number", expr))),
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
