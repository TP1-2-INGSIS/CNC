package cnc.linter

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
import cnc.ast.Statement
import cnc.ast.StringLiteral
import cnc.ast.UnaryExpression
import cnc.common.ConfigReader
import cnc.common.ErrorType
import cnc.common.Failure
import cnc.common.NamingValidator
import cnc.common.Result
import cnc.common.Success
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class LinterTest {
    private fun standardBuilder(): LinterBuilder =
        CNCLinter
            .builder()
            .registerStatement(Declaration::class, DeclarationLintRule())
            .registerStatement(Assignment::class, AssignmentLintRule)
            .registerStatement(Call::class, CallStatementLintRule())
            .registerStatement(BlockStatement::class, BlockStatementLintRule)
            .registerStatement(IfStatement::class, IfStatementLintRule)
            .registerExpression(BinaryExpression::class, BinaryExpressionLintRule)
            .registerExpression(UnaryExpression::class, UnaryExpressionLintRule)
            .registerExpression(CallExpression::class, CallExpressionLintRule())

    @Test
    fun `detects non camelCase variable declarations`() {
        val rule = DeclarationLintRule(conventionName = "camelCase", validator = CamelCaseValidator())
        val linter = CNCLinter(statementRules = mapOf(Declaration::class to rule))

        val statements =
            sequenceOf<Statement>(
                Declaration("validVariable", "number", NumberLiteral(10.0)),
                Declaration("Invalid_Variable", "string", StringLiteral("test")),
            )

        val warnings = linter.lint(statements)
        assertEquals(1, warnings.size)
        assertTrue(warnings[0].contains("Invalid_Variable"))
    }

    @Test
    fun `detects non snake_case variable declarations`() {
        val rule = DeclarationLintRule(conventionName = "snake_case", validator = SnakeCaseValidator())
        val linter = CNCLinter(statementRules = mapOf(Declaration::class to rule))

        val statements =
            sequenceOf<Statement>(
                Declaration("valid_variable", "number", NumberLiteral(10.0)),
                Declaration("invalidCamelCase", "string", StringLiteral("test")),
            )

        val warnings = linter.lint(statements)
        assertEquals(1, warnings.size)
        assertTrue(warnings[0].contains("invalidCamelCase"))
    }

    @Test
    fun `detects complex expression inside println when simple-println is enabled`() {
        val rule = CallStatementLintRule(functionName = "println", enabled = true)
        val linter = CNCLinter(statementRules = mapOf(Call::class to rule))

        val statements =
            sequenceOf<Statement>(
                Call("println", listOf(Identifier("x"))),
                Call("other", listOf(BinaryExpression(NumberLiteral(1.0), "+", NumberLiteral(2.0)))),
                Call("println", listOf(BinaryExpression(NumberLiteral(1.0), "+", NumberLiteral(2.0)))),
            )

        val warnings = linter.lint(statements)
        assertEquals(1, warnings.size)
        assertTrue(warnings[0].contains("println"))
    }

    @Test
    fun `builds linter from json configuration with default validators`() {
        val json =
            """
            {
                "naming-convention": "camelCase",
                "simple-println": true
            }
            """.trimIndent()

        val linter = (standardBuilder().build(json) as Success).data

        val statements =
            sequenceOf<Statement>(
                Declaration("bad_name", "number", NumberLiteral(1.0)),
                Call("println", listOf(BinaryExpression(Identifier("a"), "*", Identifier("b")))),
            )

        val warnings = linter.lint(statements)
        assertEquals(2, warnings.size)
    }

    @Test
    fun `supports custom injected naming validator via DeclarationLintRule`() {
        val customValidator = NamingValidator { it.startsWith("m_") }
        val json = """{ "naming-convention": "hungarian" }"""

        val rule =
            DeclarationLintRule(validators = ValidatorsContainer.empty())
                .withValidator("hungarian", customValidator)
        val linter = (CNCLinter.builder().registerStatement(rule).build(json) as Success).data

        val statements =
            sequenceOf<Statement>(
                Declaration("m_count", "number", NumberLiteral(1.0)),
                Declaration("count", "number", NumberLiteral(2.0)),
            )

        val warnings = linter.lint(statements)
        assertEquals(1, warnings.size)
        assertTrue(warnings[0].contains("count"))
    }

    @Test
    fun `supports TCK identifier_format with spaces like camel case and snake case`() {
        val camelJson = """{ "identifier_format": "camel case" }"""
        val camelLinter = (standardBuilder().build(camelJson) as Success).data
        val camelWarnings =
            camelLinter.lint(
                sequenceOf(
                    Declaration("validVar1", "number", NumberLiteral(1.0)),
                    Declaration("invalid_var", "number", NumberLiteral(2.0)),
                ),
            )
        assertEquals(1, camelWarnings.size)
        assertTrue(camelWarnings[0].contains("invalid_var"))

        val snakeJson = """{ "identifier_format": "snake case" }"""
        val snakeLinter = (standardBuilder().build(snakeJson) as Success).data
        val snakeWarnings =
            snakeLinter.lint(
                sequenceOf(
                    Declaration("valid_var_1", "number", NumberLiteral(1.0)),
                    Declaration("invalidVar", "number", NumberLiteral(2.0)),
                ),
            )
        assertEquals(1, snakeWarnings.size)
        assertTrue(snakeWarnings[0].contains("invalidVar"))
    }

    @Test
    fun `supports TCK mandatory-variable-or-literal-in-println with non-binary complex expressions`() {
        val json = """{ "mandatory-variable-or-literal-in-println": true }"""
        val linter = (standardBuilder().build(json) as Success).data

        val statements =
            sequenceOf<Statement>(
                Call("println", listOf(StringLiteral("literal ok"))),
                Call("println", listOf(Identifier("variableOk"))),
                Call("println", listOf(CallExpression("readInput", listOf(StringLiteral("Prompt"))))),
            )

        val warnings = linter.lint(statements)
        assertEquals(1, warnings.size)
        assertTrue(warnings[0].contains("println"))
    }

    @Test
    fun `supports TCK mandatory-variable-or-literal-in-readInput`() {
        val json = """{ "mandatory-variable-or-literal-in-readInput": true }"""
        val linter = (standardBuilder().build(json) as Success).data

        val statements =
            sequenceOf<Statement>(
                Declaration(
                    "name",
                    "string",
                    CallExpression(
                        "readInput",
                        listOf(
                            BinaryExpression(StringLiteral("Enter "), "+", StringLiteral("name:")),
                        ),
                    ),
                ),
                Declaration(
                    "valid",
                    "string",
                    CallExpression(
                        "readInput",
                        listOf(
                            StringLiteral("Valid prompt:"),
                        ),
                    ),
                ),
            )

        val warnings = linter.lint(statements)
        assertEquals(1, warnings.size)
        assertTrue(warnings[0].contains("readInput"))
    }

    @Test
    fun `returns Failure when naming convention validator is not found`() {
        val json = """{ "identifier_format": "unknownFormat" }"""
        val result = standardBuilder().build(json)
        assertTrue(result is Failure)
        assertTrue(result.msg.contains("unknownFormat"))
    }

    @Test
    fun `ValidatorsContainer normalizes camel and snake aliases and resolves custom keys`() {
        val container = ValidatorsContainer.default()
        assertNotNull(container.resolve("camel case"))
        assertNotNull(container.resolve("SNAKE"))
        assertNull(container.resolve("unknown"))

        val custom = ValidatorsContainer.empty().withValidator("hungarian", NamingValidator { it.startsWith("m_") })
        assertNotNull(custom.resolve("hungarian"))
    }

    @Test
    fun `supports custom StatementLintRule and ExpressionLintRule activated by aliases`() {
        data class NoEmptyBlockRule(
            val enabled: Boolean = false,
            val aliases: List<String> = listOf("no-empty-blocks", "disallow-empty-blocks"),
        ) : StatementLintRule<BlockStatement> {
            override fun lint(
                statement: BlockStatement,
                linter: CNCLinter,
            ): List<String> {
                val warnings = mutableListOf<String>()
                if (enabled && statement.statements.isEmpty()) {
                    warnings.add("Empty block is not allowed.")
                }
                warnings.addAll(BlockStatementLintRule.lint(statement, linter))
                return warnings
            }

            override fun configure(reader: ConfigReader): Result<StatementLintRule<BlockStatement>> =
                Success("ok", copy(enabled = reader.isEnabled(aliases)))
        }

        val noFalseLiteralRule =
            object : ExpressionLintRule<BooleanLiteral> {
                override fun lint(
                    expression: BooleanLiteral,
                    linter: CNCLinter,
                ): List<String> = if (!expression.value) listOf("Avoid false literal.") else emptyList()
            }

        val builder =
            CNCLinter
                .builder()
                .registerStatement<IfStatement>(IfStatementLintRule)
                .registerStatement(BlockStatement::class, NoEmptyBlockRule())
                .registerExpression<BooleanLiteral>(noFalseLiteralRule)

        val ast =
            sequenceOf<Statement>(
                IfStatement(
                    condition = BooleanLiteral(false),
                    thenBlock = BlockStatement(emptyList()),
                    elseBlock = BlockStatement(listOf(Call("println", listOf(StringLiteral("ok"))))),
                ),
            )

        val inactiveWarnings = (builder.build("{}") as Success).data.lint(ast)
        assertEquals(listOf("Avoid false literal."), inactiveWarnings)

        val activeWarnings = (builder.build("""{"disallow-empty-blocks": true}""") as Success).data.lint(ast)
        assertEquals(2, activeWarnings.size)
        assertTrue(activeWarnings.any { it.contains("Empty block") })
    }

    @Test
    fun `isolates copy mutations when extending rules`() {
        val base =
            CNCLinter
                .builder()
                .registerStatement(Declaration::class, DeclarationLintRule())
                .registerStatement(Call::class, CallStatementLintRule())

        val extended =
            base.copy().registerStatement(
                Declaration::class,
                DeclarationLintRule().withValidator("hungarian", NamingValidator { it.startsWith("m_") }),
            )
        assertTrue(base.build("""{"naming-convention": "hungarian"}""") is Failure)
        assertTrue(extended.build("""{"naming-convention": "hungarian"}""") is Success)
    }

    @Test
    fun `traverses nested expressions inside Assignment, IfStatement, UnaryExpression, and CallExpression`() {
        val linter = standardBuilder().create("""{"simple-read-input": true, "simple-println": true}""")

        val ast =
            sequenceOf<Statement>(
                Declaration("uninitialized", "number", null),
                Assignment(
                    "x",
                    UnaryExpression(
                        "!",
                        CallExpression(
                            "readInput",
                            listOf(BinaryExpression(StringLiteral("a"), "+", StringLiteral("b"))),
                        ),
                    ),
                ),
                IfStatement(
                    condition =
                        CallExpression(
                            "outer",
                            listOf(
                                CallExpression(
                                    "readInput",
                                    listOf(BinaryExpression(NumberLiteral(1.0), "+", NumberLiteral(2.0))),
                                ),
                            ),
                        ),
                    thenBlock =
                        BlockStatement(
                            listOf(
                                Call(
                                    "println",
                                    listOf(BinaryExpression(Identifier("a"), "+", Identifier("b"))),
                                ),
                            ),
                        ),
                    elseBlock = BlockStatement(emptyList()),
                ),
                IfStatement(
                    condition = BooleanLiteral(true),
                    thenBlock = BlockStatement(emptyList()),
                ),
            )

        val warnings = linter.lint(ast)
        assertEquals(3, warnings.size)
    }

    @Test
    fun `builder handles build without config, expression failures, and create fallback`() {
        val builder =
            CNCLinter
                .builder()
                .registerStatement(
                    Declaration::class,
                    DeclarationLintRule(conventionName = "camelCase", validator = CamelCaseValidator()),
                ).registerStatement(Call::class, CallStatementLintRule(enabled = true))

        val directWarnings =
            builder.build().lint(
                sequenceOf(
                    Declaration("Bad_Name", "number", NumberLiteral(1.0)),
                    Call("println", listOf(BinaryExpression(NumberLiteral(1.0), "+", NumberLiteral(2.0)))),
                ),
            )
        assertEquals(2, directWarnings.size)

        assertTrue((builder.build(null as String?) as Success).data.lint(emptySequence()).isEmpty())
        assertTrue((builder.build("not json") as Success).data.lint(emptySequence()).isEmpty())
        assertTrue(
            (
                builder.build(
                    """{"simple-println": true, "num": 1, "nil": null, "arr": [1], "str": "x"}""",
                ) as Success
            ).data.lint(sequenceOf(Call("println", listOf(BooleanLiteral(true))))).isEmpty(),
        )
        assertTrue(
            builder
                .create("""{"naming-convention": "missing"}""")
                .lint(sequenceOf(Declaration("Bad_Name", "number", NumberLiteral(1.0))))
                .isEmpty(),
        )
        assertTrue((builder.build(ConfigReader.EMPTY) as Success).data.lint(emptySequence()).isEmpty())

        val failingExprBuilder =
            CNCLinter.builder().registerExpression(
                NumberLiteral::class,
                object : ExpressionLintRule<NumberLiteral> {
                    override fun lint(
                        expression: NumberLiteral,
                        linter: CNCLinter,
                    ): List<String> = emptyList()

                    override fun configure(reader: ConfigReader): Result<ExpressionLintRule<NumberLiteral>> =
                        Failure("bad expr config", ErrorType.CLI)
                },
            )
        assertTrue(failingExprBuilder.build("{}") is Failure)
        assertTrue(CNCLinter().lint(Declaration("x", "number", NumberLiteral(1.0))).isEmpty())
    }
}
