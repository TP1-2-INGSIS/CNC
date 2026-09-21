package cnc.linter

import cnc.ast.BinaryExpression
import cnc.ast.Call
import cnc.ast.Declaration
import cnc.ast.Identifier
import cnc.ast.NumberLiteral
import cnc.ast.Statement
import cnc.ast.StringLiteral
import cnc.common.Failure
import cnc.common.Success
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
        val rule = SimpleFunctionCallRule("println")
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

        val linter = (LinterFactory.build(json, validators) as Success).data

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

        val linter = (LinterFactory.build(json, customValidators) as Success).data

        val statements = sequenceOf<Statement>(
            Declaration("m_count", "number", NumberLiteral(1.0)),
            Declaration("count", "number", NumberLiteral(2.0))
        )

        val warnings = linter.lint(statements)
        assertEquals(1, warnings.size)
        assertTrue(warnings[0].contains("count"))
    }

    @Test
    fun `supports TCK identifier_format with spaces like camel case and snake case`() {
        val camelJson = """{ "identifier_format": "camel case" }"""
        val camelLinter = (LinterFactory.build(camelJson, validators) as Success).data
        val camelWarnings = camelLinter.lint(sequenceOf(
            Declaration("validVar1", "number", NumberLiteral(1.0)),
            Declaration("invalid_var", "number", NumberLiteral(2.0))
        ))
        assertEquals(1, camelWarnings.size)
        assertTrue(camelWarnings[0].contains("invalid_var"))

        val snakeJson = """{ "identifier_format": "snake case" }"""
        val snakeLinter = (LinterFactory.build(snakeJson, validators) as Success).data
        val snakeWarnings = snakeLinter.lint(sequenceOf(
            Declaration("valid_var_1", "number", NumberLiteral(1.0)),
            Declaration("invalidVar", "number", NumberLiteral(2.0))
        ))
        assertEquals(1, snakeWarnings.size)
        assertTrue(snakeWarnings[0].contains("invalidVar"))
    }

    @Test
    fun `supports TCK mandatory-variable-or-literal-in-println with non-binary complex expressions`() {
        val json = """{ "mandatory-variable-or-literal-in-println": true }"""
        val linter = (LinterFactory.build(json, validators) as Success).data

        val statements = sequenceOf<Statement>(
            Call("println", listOf(StringLiteral("literal ok"))),
            Call("println", listOf(Identifier("variableOk"))),
            Call("println", listOf(cnc.ast.CallExpression("readInput", listOf(StringLiteral("Prompt")))))
        )

        val warnings = linter.lint(statements)
        assertEquals(1, warnings.size)
        assertTrue(warnings[0].contains("println"))
    }

    @Test
    fun `supports TCK mandatory-variable-or-literal-in-readInput`() {
        val json = """{ "mandatory-variable-or-literal-in-readInput": true }"""
        val linter = (LinterFactory.build(json, validators) as Success).data

        val statements = sequenceOf<Statement>(
            Declaration("name", "string", cnc.ast.CallExpression("readInput", listOf(
                BinaryExpression(StringLiteral("Enter "), "+", StringLiteral("name:"))
            ))),
            Declaration("valid", "string", cnc.ast.CallExpression("readInput", listOf(
                StringLiteral("Valid prompt:")
            )))
        )

        val warnings = linter.lint(statements)
        assertEquals(1, warnings.size)
        assertTrue(warnings[0].contains("readInput"))
    }

    @Test
    fun `returns Failure when naming convention validator is not found`() {
        val json = """{ "identifier_format": "unknownFormat" }"""
        val result = LinterFactory.build(json, validators)
        assertTrue(result is Failure)
        assertTrue(result.msg.contains("unknownFormat"))
    }
}
