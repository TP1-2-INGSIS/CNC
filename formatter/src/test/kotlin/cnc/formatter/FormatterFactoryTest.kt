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
import cnc.common.Failure
import cnc.common.Success
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

class FormatterFactoryTest {
    @Nested
    inner class BuildAndCreateTests {
        @Test
        fun `build with null, empty, or whitespace returns default formatter`() {
            val res1 = FormatterFactory.build(null)
            assertTrue(res1 is Success)

            val res2 = FormatterFactory.build("")
            assertTrue(res2 is Success)

            val res3 = FormatterFactory.build("   ")
            assertTrue(res3 is Success)
        }

        @Test
        fun `build with invalid json returns Failure`() {
            val res = FormatterFactory.build("not valid json")
            assertTrue(res is Failure)
        }

        @Test
        fun `create returns formatter on success and fallback on failure`() {
            val fmtValid = FormatterFactory.create("{}")
            assertNotNull(fmtValid)

            val fmtInvalid = FormatterFactory.create("not json")
            assertNotNull(fmtInvalid)
        }
    }

    @Nested
    inner class FormatterAstRulesTests {
        @Test
        fun `formats variable declarations mutable and const with and without init value`() {
            val json = """{"space-before-colon": false, "space-after-colon": true}"""
            val formatter = FormatterFactory.create(json)

            val letDecl = Declaration("x", "number", NumberLiteral(10.0), isMutable = true)
            assertEquals("let x: number = 10;", formatter.format(listOf(letDecl)))

            val constDecl = Declaration("PI", "number", NumberLiteral(3.14), isMutable = false)
            assertEquals("const PI: number = 3.14;", formatter.format(listOf(constDecl)))

            val noValDecl = Declaration("y", "string", null, isMutable = true)
            assertEquals("let y: string;", formatter.format(listOf(noValDecl)))
        }

        @Test
        fun `formats assignment and function call with println line breaks`() {
            val json = """{"line-breaks-after-println": 2}"""
            val formatter = FormatterFactory.create(json)

            val assign = Assignment("x", NumberLiteral(20.0))
            assertEquals("x = 20;", formatter.format(listOf(assign)))

            val callPrintln = Call("println", listOf(StringLiteral("hello")))
            assertEquals("println(\"hello\");\n\n", formatter.format(listOf(callPrintln)))

            val otherCall = Call("myFunc", listOf(Identifier("x")))
            assertEquals("myFunc(x);", formatter.format(listOf(otherCall)))
        }

        @Test
        fun `formats single space separation mode for call, call expression, and colons`() {
            val json = """{"single-space-separation": true}"""
            val formatter = FormatterFactory.create(json)

            val call = Call("println", listOf(StringLiteral("hi")))
            assertEquals("println ( \"hi\" );", formatter.format(listOf(call)))

            val emptyCall = Call("flush", emptyList())
            assertEquals("flush();", formatter.format(listOf(emptyCall)))

            val decl = Declaration("a", "number", NumberLiteral(1.0))
            assertEquals("let a : number = 1;", formatter.format(listOf(decl)))

            val declCallExpr = Declaration("name", "string", CallExpression("readInput", listOf(StringLiteral("?"))))
            assertEquals("let name : string = readInput ( \"?\" );", formatter.format(listOf(declCallExpr)))
        }

        @Test
        fun `formats colon, equals, and operator spacing variations`() {
            val bothSpacing = FormatterFactory.create("""{"space-before-colon": true, "space-after-colon": true}""")
            val decl = Declaration("a", "number", null)
            assertEquals("let a : number;", bothSpacing.format(listOf(decl)))

            val beforeOnly = FormatterFactory.create("""{"space-before-colon": true, "space-after-colon": false}""")
            assertEquals("let a :number;", beforeOnly.format(listOf(decl)))

            val noSpaceAroundEq = FormatterFactory.create("""{"no-space-around-equals": true}""")
            val declEq = Declaration("a", "number", NumberLiteral(5.0))
            assertEquals("let a: number=5;", noSpaceAroundEq.format(listOf(declEq)))
            assertEquals("a=5;", noSpaceAroundEq.format(listOf(Assignment("a", NumberLiteral(5.0)))))

            val composed =
                FormatterFactory.create(
                    """
                    {
                      "space-before-colon": true,
                      "space-after-colon": false,
                      "no-space-around-equals": true,
                      "mandatory-space-surrounding-operations": true
                    }
                    """.trimIndent(),
                )
            val expr = BinaryExpression(NumberLiteral(1.0), "+", NumberLiteral(2.0))
            assertEquals("let a :number=1 + 2;", composed.format(listOf(Declaration("a", "number", expr))))

            val forceEqualsSpace =
                StandardFormatRules
                    .v1_0Builder()
                    .registerStatement<Assignment>(AssignmentRule(assignSpacing = Spacing.NONE))
                    .create("""{"space-around-equals": true}""")
            assertEquals("x = 1;", forceEqualsSpace.format(listOf(Assignment("x", NumberLiteral(1.0)))))

            val forceOpSpace =
                StandardFormatRules
                    .v1_0Builder()
                    .registerExpression<BinaryExpression>(BinaryExpressionRule(operatorSpacing = Spacing.NONE))
                    .create("""{"mandatory-space-surrounding-operations": true}""")
            assertEquals("let a: number = 1 + 2;", forceOpSpace.format(listOf(Declaration("a", "number", expr))))
        }

        @Test
        fun `formats empty block and nested block statement`() {
            val formatter = FormatterFactory.create("""{"indent-inside-if": 4}""")

            val emptyBlock = BlockStatement(emptyList())
            assertEquals("{}", formatter.format(listOf(emptyBlock)))

            val innerDecl = Declaration("x", "number", NumberLiteral(1.0))
            val block = BlockStatement(listOf(innerDecl))
            assertEquals("{\n    let x: number = 1;\n}", formatter.format(listOf(block)))

            val zeroIndent = FormatterFactory.create("""{"indent-inside-if": 0}""")
            assertEquals("{\nlet x: number = 1;\n}", zeroIndent.format(listOf(block)))
        }

        @Test
        fun `formats if statement with and without else and brace position options`() {
            val sameLineFmt = FormatterFactory.create("""{"if-brace-below-line": false}""")
            val ifStmtNoElse =
                IfStatement(
                    condition = BooleanLiteral(true),
                    thenBlock = BlockStatement(listOf(Call("println", listOf(StringLiteral("yes"))))),
                )
            val expectedSameLine = "if (true) {\n  println(\"yes\");\n}"
            assertEquals(expectedSameLine, sameLineFmt.format(listOf(ifStmtNoElse)))

            val belowLineFmt = FormatterFactory.create("""{"if-brace-below-line": true}""")
            val ifStmtWithElse =
                IfStatement(
                    condition = BooleanLiteral(false),
                    thenBlock = BlockStatement(emptyList()),
                    elseBlock = BlockStatement(emptyList()),
                )
            assertEquals("if (false)\n{} else {}", belowLineFmt.format(listOf(ifStmtWithElse)))

            val explicitSameLine =
                StandardFormatRules
                    .v1_1Builder()
                    .registerStatement<IfStatement>(IfStatementRule(braceBelowLine = true))
                    .create("""{"if-brace-same-line": true}""")
            assertEquals("if (false) {} else {}", explicitSameLine.format(listOf(ifStmtWithElse)))
        }

        @Test
        fun `composes single-space-separation with line-breaks-after-println`() {
            val fmt =
                FormatterFactory.create(
                    """{"single-space-separation": true, "line-breaks-after-println": 1}""",
                )
            val ast =
                listOf(
                    Call("println", listOf(StringLiteral("a"))),
                    Call("println", listOf(StringLiteral("b"))),
                )
            assertEquals("println ( \"a\" );\n\nprintln ( \"b\" );\n", fmt.format(ast))
        }
    }

    @Nested
    inner class FormatSourceTests {
        @Test
        fun `formats source via injected parse`() {
            val statements = listOf(Declaration("x", "number", NumberLiteral(10.0), isMutable = true))
            val result = FormatterFactory.formatSource("let x:number=10;", null) { statements }
            assertEquals("let x: number = 10;", result)
        }

        @Test
        fun `applies config through formatSource`() {
            val statements = listOf(Declaration("a", "number", NumberLiteral(1.0), isMutable = true))
            val json = """{"single-space-separation": true}"""
            val result = FormatterFactory.formatSource("let a:number=1;", json) { statements }
            assertEquals("let a : number = 1;", result)
        }

        @Test
        fun `returns raw source when parse yields null`() {
            val src = "let x = ;"
            val result = FormatterFactory.formatSource(src, null) { null }
            assertEquals(src, result)
        }

        @Test
        fun `returns raw source when parse throws`() {
            val src = "@@ invalid @@"
            val result = FormatterFactory.formatSource(src, null) { error("boom") }
            assertEquals(src, result)
        }
    }

    @Nested
    inner class StandardFormatRulesExtraTests {
        private val formatter = StandardFormatRules.defaultFormatter()

        @Test
        fun `formats unary and boolean expressions and function calls`() {
            val unary = Declaration("neg", "number", UnaryExpression("-", NumberLiteral(5.0)))
            assertEquals("let neg: number = -5;", formatter.format(listOf(unary)))

            val notExpr =
                Declaration(
                    "flag",
                    "boolean",
                    UnaryExpression("!", BinaryExpression(Identifier("a"), "+", Identifier("b"))),
                )
            assertEquals("let flag: boolean = !(a + b);", formatter.format(listOf(notExpr)))

            val boolVal = Declaration("flag", "boolean", BooleanLiteral(true))
            assertEquals("let flag: boolean = true;", formatter.format(listOf(boolVal)))

            val callExpr = Declaration("read", "string", CallExpression("readInput", listOf(StringLiteral("Enter:"))))
            assertEquals("let read: string = readInput(\"Enter:\");", formatter.format(listOf(callExpr)))
        }

        @Test
        fun `formats default block and if statement`() {
            val ifStmt =
                IfStatement(
                    condition = Identifier("flag"),
                    thenBlock = BlockStatement(listOf(Call("println", listOf(StringLiteral("A"))))),
                    elseBlock = BlockStatement(listOf(Call("println", listOf(StringLiteral("B"))))),
                )
            val formatted = formatter.format(listOf(ifStmt))
            assertTrue(formatted.contains("if (flag) {\n    println(\"A\");\n} else {\n    println(\"B\");\n}"))
        }
    }
}
