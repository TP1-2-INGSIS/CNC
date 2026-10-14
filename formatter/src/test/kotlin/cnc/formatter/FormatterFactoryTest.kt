package cnc.formatter

import cnc.ast.Assignment
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
        fun `formats single space separation mode for call and colons`() {
            val json = """{"single-space-separation": true}"""
            val formatter = FormatterFactory.create(json)

            val call = Call("println", listOf(StringLiteral("hi")))
            assertEquals("println ( \"hi\" );", formatter.format(listOf(call)))

            val decl = Declaration("a", "number", NumberLiteral(1.0))
            assertEquals("let a : number = 1;", formatter.format(listOf(decl)))
        }

        @Test
        fun `formats colon spacing variations`() {
            val bothSpacing = FormatterFactory.create("""{"space-before-colon": true, "space-after-colon": true}""")
            val decl = Declaration("a", "number", null)
            assertEquals("let a : number;", bothSpacing.format(listOf(decl)))

            val beforeOnly = FormatterFactory.create("""{"space-before-colon": true, "space-after-colon": false}""")
            assertEquals("let a :number;", beforeOnly.format(listOf(decl)))

            val noSpaceAroundEq = FormatterFactory.create("""{"no-space-around-equals": true}""")
            val declEq = Declaration("a", "number", NumberLiteral(5.0))
            assertEquals("let a: number=5;", noSpaceAroundEq.format(listOf(declEq)))
        }

        @Test
        fun `formats empty block and nested block statement`() {
            val formatter = FormatterFactory.create("""{"indent-inside-if": 4}""")

            val emptyBlock = BlockStatement(emptyList())
            assertEquals("{}", formatter.format(listOf(emptyBlock)))

            val innerDecl = Declaration("x", "number", NumberLiteral(1.0))
            val block = BlockStatement(listOf(innerDecl))
            assertEquals("{\n    let x: number = 1;\n}", formatter.format(listOf(block)))
        }

        @Test
        fun `formats if statement with and without else and brace below line`() {
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
        }
    }

    @Nested
    inner class FormatSourceTests {
        @Test
        fun `formatSource returns raw source if config is null or invalid`() {
            val src = "let x=10;"
            assertEquals(src, FormatterFactory.formatSource(src, null))
            assertEquals(src, FormatterFactory.formatSource(src, ""))
            assertEquals(src, FormatterFactory.formatSource(src, "not json"))
        }

        @Test
        fun `formatSource line break after statement`() {
            val src = "let a: number = 1; let b: number = 2;"
            val json = """{"mandatory-line-break-after-statement": true}"""
            val res = FormatterFactory.formatSource(src, json)
            assertTrue(res.contains("let a: number = 1;\nlet b: number = 2;"))
        }

        @Test
        fun `formatSource space around equals`() {
            val src = "let a=1;"
            val withSpaces = FormatterFactory.formatSource(src, """{"enforce-spacing-around-equals": true}""")
            assertEquals("let a = 1;", withSpaces)

            val noSpaces = FormatterFactory.formatSource("let a = 1;", """{"enforce-no-spacing-around-equals": true}""")
            assertEquals("let a=1;", noSpaces)
        }

        @Test
        fun `formatSource colon spacing in declaration`() {
            val src = "let x:number=1;"
            val spaceBefore =
                FormatterFactory.formatSource(
                    src,
                    """{"enforce-spacing-before-colon-in-declaration": true}""",
                )
            assertTrue(spaceBefore.contains("let x :"))

            val spaceAfter =
                FormatterFactory.formatSource(
                    src,
                    """{"enforce-spacing-after-colon-in-declaration": true}""",
                )
            assertTrue(spaceAfter.contains(": number"))
        }

        @Test
        fun `formatSource space surrounding operations`() {
            val src = "let res: number = a+b*c-d/e;"
            val json = """{"mandatory-space-surrounding-operations": true}"""
            val formatted = FormatterFactory.formatSource(src, json)
            assertEquals("let res: number = a + b * c - d / e;", formatted)
        }

        @Test
        fun `formatSource single space separation`() {
            val src = "let x:number=10;\nprintln(x);"
            val json = """{"mandatory-single-space-separation": true}"""
            val formatted = FormatterFactory.formatSource(src, json)
            assertTrue(formatted.contains("let x : number = 10;"))
            assertTrue(formatted.contains("println ( x );"))
        }

        @Test
        fun `formatSource if brace position and indentation`() {
            val src = "if (true) {\nlet a = 1;\n}"
            val below = FormatterFactory.formatSource(src, """{"if-brace-below-line": true}""")
            assertTrue(below.contains("if (true)\n{"))

            val same = FormatterFactory.formatSource("if (true)\n{\nlet a = 1;\n}", """{"if-brace-same-line": true}""")
            assertTrue(same.contains("if (true) {"))

            val indented = FormatterFactory.formatSource("if (true) {\nlet a = 1;\n}", """{"indent-inside-if": 4}""")
            assertTrue(indented.contains("    let a = 1;"))
        }

        @Test
        fun `formatSource println line breaks`() {
            val src = "println(\"1\");\nprintln(\"2\");"
            val json = """{"line-breaks-after-println": 2}"""
            val formatted = FormatterFactory.formatSource(src, json)
            assertTrue(formatted.contains("println(\"1\");\n\n\nprintln(\"2\");"))

            val singleStmt = "println(\"end\");"
            val fmtSingle = FormatterFactory.formatSource(singleStmt, json)
            assertEquals("println(\"end\");", fmtSingle)
        }
    }

    @Nested
    inner class StandardFormatRulesExtraTests {
        private val formatter = StandardFormatRules.defaultFormatter()

        @Test
        fun `formats unary and boolean expressions and function calls`() {
            val unary = Declaration("neg", "number", UnaryExpression("-", NumberLiteral(5.0)))
            assertEquals("let neg: number = -5;", formatter.format(listOf(unary)))

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
