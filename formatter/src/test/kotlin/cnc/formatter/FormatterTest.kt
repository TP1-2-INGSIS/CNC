package cnc.formatter

import cnc.ast.BinaryExpression
import cnc.ast.BlockStatement
import cnc.ast.BooleanLiteral
import cnc.ast.Call
import cnc.ast.CallExpression
import cnc.ast.Declaration
import cnc.ast.Expression
import cnc.ast.Identifier
import cnc.ast.IfStatement
import cnc.ast.NumberLiteral
import cnc.ast.Statement
import cnc.ast.StringLiteral
import cnc.common.ConfigReader
import cnc.common.Failure
import cnc.common.Success
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

class FormatterTest {
    private val precedences = mapOf("+" to 1, "*" to 2)

    private val customBuilder =
        Formatter
            .builder()
            .registerExpression<NumberLiteral>(NumberLiteralRule)
            .registerExpression<StringLiteral>(StringLiteralRule)
            .registerExpression<Identifier>(IdentifierRule)
            .registerExpression<BinaryExpression>(BinaryExpressionRule(precedences))
            .registerStatement<Declaration>(DeclarationRule())

    private fun formatter() = customBuilder.build()

    private fun decl(
        name: String,
        type: String,
        value: Expression,
    ) = Declaration(name = name, type = type, value = value)

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
        val inner = BinaryExpression(NumberLiteral(1.0), "+", NumberLiteral(2.0))
        val expr = BinaryExpression(inner, "*", NumberLiteral(3.0))
        val ast = listOf(decl("z", "number", expr))
        assertEquals("let z: number = (1 + 2) * 3;", formatter().format(ast))
    }

    @Test
    fun `omits parentheses when child precedence is higher than parent`() {
        val inner = BinaryExpression(NumberLiteral(2.0), "*", NumberLiteral(3.0))
        val expr = BinaryExpression(NumberLiteral(1.0), "+", inner)
        val ast = listOf(decl("z", "number", expr))
        assertEquals("let z: number = 1 + 2 * 3;", formatter().format(ast))
    }

    @Test
    fun `formats multiple statements separated by newline or custom separator`() {
        val ast =
            listOf(
                decl("a", "number", NumberLiteral(1.0)),
                decl("b", "string", StringLiteral("hi")),
            )
        assertEquals("let a: number = 1;\nlet b: string = \"hi\";", formatter().format(ast))

        val singleLineFormatter = customBuilder.copy().withStatementSeparator(" ").build()
        assertEquals("let a: number = 1; let b: string = \"hi\";", singleLineFormatter.format(ast))
    }

    @Test
    fun `fails when no statement rule or expression rule applies`() {
        val noStmt = Formatter.builder().registerExpressions(mapOf(NumberLiteral::class to NumberLiteralRule)).build()
        val ast = listOf(decl("x", "number", NumberLiteral(5.0)))
        assertFailsWith<IllegalStateException> { noStmt.format(ast) }

        val noExpr = Formatter.builder().registerStatements(mapOf(Declaration::class to DeclarationRule())).build()
        assertFailsWith<IllegalStateException> { noExpr.format(ast) }
    }

    @Test
    fun `fails when binary operator precedence is missing`() {
        val fmt =
            Formatter
                .builder()
                .registerStatement<Declaration>(DeclarationRule())
                .registerExpression<NumberLiteral>(NumberLiteralRule)
                .registerExpression<BinaryExpression>(BinaryExpressionRule(mapOf("+" to 1)))
                .build()

        val unknownRootOp = BinaryExpression(NumberLiteral(1.0), "%", NumberLiteral(2.0))
        assertFailsWith<IllegalStateException> { fmt.format(listOf(decl("x", "number", unknownRootOp))) }

        val unknownChildOp =
            BinaryExpression(BinaryExpression(NumberLiteral(1.0), "%", NumberLiteral(2.0)), "+", NumberLiteral(3.0))
        assertFailsWith<IllegalStateException> { fmt.format(listOf(decl("x", "number", unknownChildOp))) }
    }

    @Test
    fun `builder copy isolates mutations from the original builder`() {
        val base = TestFormatterFactory.baseBuilder()
        val custom =
            base
                .copy()
                .registerStatement(
                    Declaration::class,
                    DeclarationRule(colonSpacing = Spacing.NONE, assignSpacing = Spacing.NONE),
                )

        val node = listOf(decl("x", "number", NumberLiteral(1.0)))
        assertEquals("let x: number = 1;", base.build().format(node))
        assertEquals("let x:number=1;", custom.build().format(node))
    }

    @Test
    fun `supports custom self-configuring StatementFormatRule and ExpressionFormatRule`() {
        class CustomCallRule(
            private val prefix: String = "",
        ) : StatementFormatRule<Call> {
            override fun format(
                statement: Call,
                formatter: Formatter,
            ): String {
                val args = statement.arguments.joinToString(", ") { formatter.format(it) }
                return "$prefix${statement.function}($args);"
            }

            override fun configure(reader: ConfigReader): StatementFormatRule<Call> =
                reader.readString(listOf("call-prefix"))?.let { CustomCallRule("$it ") } ?: this
        }

        class CustomNumberRule(
            private val suffix: String = "",
        ) : ExpressionFormatRule<NumberLiteral> {
            override fun format(
                expression: NumberLiteral,
                formatter: Formatter,
            ): String = "${NumberLiteralRule.format(expression, formatter)}$suffix"

            override fun configure(reader: ConfigReader): ExpressionFormatRule<NumberLiteral> =
                if (reader.isEnabled(listOf("number-decimal-suffix"))) CustomNumberRule(".0") else this
        }

        val customBuilder =
            TestFormatterFactory
                .baseBuilder()
                .registerStatements(mapOf(Call::class to CustomCallRule()))
                .registerExpressions(mapOf(NumberLiteral::class to CustomNumberRule()))

        val configured = customBuilder.create("""{"call-prefix": "/* call */", "number-decimal-suffix": true}""")
        val ast: List<Statement> =
            listOf(
                decl("x", "number", NumberLiteral(10.0)),
                Call("println", listOf(Identifier("x"))),
            )
        assertEquals("let x: number = 10.0;\n/* call */ println(x);", configured.format(ast))

        val defaults = customBuilder.create("{}")
        assertEquals("let x: number = 10;\nprintln(x);", defaults.format(ast))
    }

    @Test
    fun `baseBuilder rejects unregistered nodes and does not leak CallExpression under single-space config`() {
        val baseFmt = TestFormatterFactory.baseBuilder().create("""{"single-space-separation": true}""")

        assertFailsWith<IllegalStateException> {
            baseFmt.format(listOf(BlockStatement(emptyList())))
        }
        assertFailsWith<IllegalStateException> {
            baseFmt.format(listOf(IfStatement(BooleanLiteral(true), BlockStatement(emptyList()))))
        }
        assertFailsWith<IllegalStateException> {
            baseFmt.format(listOf(decl("flag", "boolean", BooleanLiteral(true))))
        }
        assertFailsWith<IllegalStateException> {
            baseFmt.format(listOf(decl("name", "string", CallExpression("readInput", listOf(StringLiteral("?"))))))
        }
    }

    @Test
    fun `build parses ConfigReader and all JSON primitive variants safely`() {
        val builder = TestFormatterFactory.fullBuilder()
        val fromReader = builder.build(ConfigReader.fromMap(mapOf("indent-inside-if" to 3)))
        val block = BlockStatement(listOf(Call("println", listOf(NumberLiteral(1.0)))))
        assertEquals("{\n   println(1);\n}", fromReader.format(listOf(block)))

        val jsonWithMixedTypes =
            """
            {
              "space-around-equals": true,
              "indent-inside-if": 4,
              "ignored-null": null,
              "ignored-array": [1, 2],
              "ignored-str": "abc"
            }
            """.trimIndent()
        val res = builder.build(jsonWithMixedTypes)
        assertTrue(res is Success)
        assertEquals("{\n    println(1);\n}", res.data.format(listOf(block)))

        assertTrue(builder.build("null") is Failure)
        assertFalse(builder.build("[1, 2]") is Success)
        assertNotEquals(
            builder.build().format(listOf(block)),
            builder.create("{}").format(listOf(block)),
        )
    }

    @Test
    fun `configure returns same instance when no config key alters rule state`() {
        val emptyReader = ConfigReader.EMPTY
        val binary = BinaryExpressionRule()
        assertEquals(binary, binary.configure(emptyReader))

        val callExpr = CallExpressionRule()
        assertEquals(callExpr, callExpr.configure(emptyReader))

        val assign = AssignmentRule()
        assertEquals(assign, assign.configure(emptyReader))

        val declRule = DeclarationRule()
        assertEquals(declRule, declRule.configure(emptyReader))

        val callStmt = CallStatementRule()
        assertEquals(callStmt, callStmt.configure(emptyReader))

        val ifStmt = IfStatementRule()
        assertEquals(ifStmt, ifStmt.configure(emptyReader))

        val blockStmt = BlockStatementRule(indentSize = StandardFormatRules.JSON_FALLBACK_INDENT)
        assertEquals(blockStmt, blockStmt.configure(emptyReader))
    }
}
