package cnc.linter

import cnc.ast.BinaryExpression
import cnc.ast.Call
import cnc.ast.Declaration
import cnc.ast.Identifier
import cnc.ast.NumberLiteral
import cnc.ast.Statement
import cnc.ast.StringLiteral
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class LinterTest {

    private val validators = mapOf(
        "camelCase" to CamelCaseValidator(),
        "snake_case" to SnakeCaseValidator()
    )

    @Test
    fun `detects non camelCase variable declarations`() {
        val rule = NamingConventionRule("camelCase", CamelCaseValidator())
        val linter = CNCLinter(listOf(rule))

        val statements = sequenceOf<Statement>(
            Declaration("validVariable", "number", NumberLiteral(10.0)),
            Declaration("Invalid_Variable", "string", StringLiteral("test"))
        )

        val warnings = linter.lint(statements)
        assertEquals(1, warnings.size)
        assertTrue(warnings[0].contains("Invalid_Variable"))
    }

    @Test
    fun `detects non snake_case variable declarations`() {
        val rule = NamingConventionRule("snake_case", SnakeCaseValidator())
        val linter = CNCLinter(listOf(rule))

        val statements = sequenceOf<Statement>(
            Declaration("valid_variable", "number", NumberLiteral(10.0)),
            Declaration("invalidCamelCase", "string", StringLiteral("test"))
        )

        val warnings = linter.lint(statements)
        assertEquals(1, warnings.size)
        assertTrue(warnings[0].contains("invalidCamelCase"))
    }

    @Test
    fun `detects complex expression inside println when simple-println is enabled`() {
        val rule = SimplePrintlnRule()
        val linter = CNCLinter(listOf(rule))

        val statements = sequenceOf<Statement>(
            Call("println", listOf(Identifier("x"))),
            Call("println", listOf(BinaryExpression(NumberLiteral(1.0), "+", NumberLiteral(2.0))))
        )

        val warnings = linter.lint(statements)
        assertEquals(1, warnings.size)
        assertTrue(warnings[0].contains("println"))
    }

    @Test
    fun `builds linter from json configuration with injected validators`() {
        val json = """
            {
                "naming-convention": "camelCase",
                "simple-println": true
            }
        """.trimIndent()

        val linter = LinterFactory.build(json, validators)

        val statements = sequenceOf<Statement>(
            Declaration("bad_name", "number", NumberLiteral(1.0)),
            Call("println", listOf(BinaryExpression(Identifier("a"), "*", Identifier("b"))))
        )

        val warnings = linter.lint(statements)
        assertEquals(2, warnings.size)
    }

    @Test
    fun `supports custom injected naming validator`() {
        val customValidator = NamingValidator { it.startsWith("m_") }
        val customValidators = mapOf("hungarian" to customValidator)

        val json = """
            {
                "naming-convention": "hungarian"
            }
        """.trimIndent()

        val linter = LinterFactory.build(json, customValidators)

        val statements = sequenceOf<Statement>(
            Declaration("m_count", "number", NumberLiteral(1.0)),
            Declaration("count", "number", NumberLiteral(2.0))
        )

        val warnings = linter.lint(statements)
        assertEquals(1, warnings.size)
        assertTrue(warnings[0].contains("count"))
    }
}
