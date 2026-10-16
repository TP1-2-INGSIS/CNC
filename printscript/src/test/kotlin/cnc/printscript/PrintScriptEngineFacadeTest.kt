package cnc.printscript

import cnc.ast.Declaration
import cnc.ast.NumberLiteral
import cnc.common.Failure
import cnc.common.FileContent
import cnc.common.InputStreamContent
import cnc.common.StringContent
import cnc.common.Success
import cnc.language.LanguageFacade
import cnc.printscript.profile.PrintScriptProfileRegistry
import cnc.printscript.profile.v1_0.PrintScript10Profile
import cnc.printscript.profile.v1_1.PrintScript11Profile
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import kotlin.io.path.createTempFile

class PrintScriptFacadeTest {
    private val engine10: LanguageFacade = PsVersioner.version("1.0")
    private val engine11: LanguageFacade = PsVersioner.version("1.1")

    @Nested
    inner class VersionAndRegistryTests {
        @Test
        fun `resolves versions correctly and throws on invalid`() {
            assertEquals(PrintScriptVersion.V1_0, PrintScriptVersion.from("1.0"))
            assertEquals(PrintScriptVersion.V1_1, PrintScriptVersion.from(" 1.1 "))

            assertThrows(UnsupportedVersionException::class.java) {
                PrintScriptVersion.from("9.9")
            }
        }

        @Test
        fun `profiles expose their version as a LanguageVersion`() {
            assertEquals("1.0", PrintScript10Profile.version.label)
            assertEquals("1.1", PrintScript11Profile.version.label)
            assertSame(PrintScriptVersion.V1_0, PrintScript10Profile.version)
            assertSame(PrintScriptVersion.V1_1, PrintScript11Profile.version)
        }

        @Test
        fun `engine factory methods for versions`() {
            val e1 = PrintScriptFacade(PrintScriptVersion.V1_0)
            val e2 = PrintScriptFacade(PrintScriptVersion.V1_1)
            assertNotNull(e1)
            assertNotNull(e2)

            val p1 = PrintScriptProfileRegistry.forVersion(PrintScriptVersion.V1_0)
            val p2 = PrintScriptProfileRegistry.forVersion(PrintScriptVersion.V1_1)
            assertSame(PrintScript10Profile, p1)
            assertSame(PrintScript11Profile, p2)
        }
    }

    @Nested
    inner class FacadeOverloadsTests {
        private val sampleCode = "let x: number = 42;\nprintln(x);"

        @Test
        fun `execute with String, File, InputStream content and custom IO`() {
            val outputList = mutableListOf<String>()
            val resStr =
                engine11.execute(
                    content = StringContent(sampleCode),
                    input = { _: String -> "input" },
                    envProvider = { _: String -> null },
                    output = { str: String -> outputList.add(str) },
                )
            assertTrue(resStr.isOk())
            assertEquals(listOf("42"), outputList)

            val tempFile = createTempFile("ps_exec", ".prs").toFile()
            tempFile.deleteOnExit()
            tempFile.writeText(sampleCode)

            assertTrue(engine11.execute(FileContent(tempFile.absolutePath)).isOk())
            assertTrue(engine11.execute(InputStreamContent(sampleCode.byteInputStream())).isOk())
            assertTrue(engine11.execute(StringContent(sampleCode)).isOk())
        }

        @Test
        fun `validate with String, File, InputStream content and parsed statements`() {
            val tempFile = createTempFile("ps_val", ".prs").toFile()
            tempFile.deleteOnExit()
            tempFile.writeText(sampleCode)

            assertTrue(engine11.validate(StringContent(sampleCode)).isOk())
            assertTrue(engine11.validate(FileContent(tempFile.absolutePath)).isOk())
            assertTrue(engine11.validate(InputStreamContent(sampleCode.byteInputStream())).isOk())

            val decl = Declaration("x", "number", NumberLiteral(42.0))
            assertTrue(engine11.semantic(listOf(decl)).isOk())
        }

        @Test
        fun `format with String, File, InputStream content and statements`() {
            val raw = "let x:number=42;"
            val tempFile = createTempFile("ps_fmt", ".prs").toFile()
            tempFile.deleteOnExit()
            tempFile.writeText(raw)

            val expected = "let x: number = 42;"
            assertEquals(expected, engine11.format(StringContent(raw)))
            assertEquals(expected, engine11.format(FileContent(tempFile.absolutePath)))
            assertEquals(expected, engine11.format(InputStreamContent(raw.byteInputStream())))
        }

        @Test
        fun `lint with config string over different content sources`() {
            val raw = "let x: number = 42;"
            val tempFile = createTempFile("ps_lint", ".prs").toFile()
            tempFile.deleteOnExit()
            tempFile.writeText(raw)

            assertEquals(emptyList<String>(), engine11.lint(StringContent(raw), "{}"))
            assertEquals(emptyList<String>(), engine11.lint(FileContent(tempFile.absolutePath), "{}"))
            assertEquals(emptyList<String>(), engine11.lint(InputStreamContent(raw.byteInputStream()), "{}"))
        }

        @Test
        fun `lex, parse, semantic, and interpret pipeline stages`() {
            val tempFile = createTempFile("ps_stages", ".prs").toFile()
            tempFile.deleteOnExit()
            tempFile.writeText(sampleCode)

            // lex
            assertTrue(engine11.lex(StringContent(sampleCode)).toList().isNotEmpty())
            assertTrue(engine11.lex(FileContent(tempFile.absolutePath)).toList().isNotEmpty())
            assertTrue(engine11.lex(InputStreamContent(sampleCode.byteInputStream())).toList().isNotEmpty())

            // parse
            assertTrue(engine11.parse(engine11.lex(StringContent(sampleCode))).isOk())
            assertTrue(engine11.parse(engine11.lex(FileContent(tempFile.absolutePath))).isOk())

            // semantic
            val statements = (engine11.parse(engine11.lex(StringContent(sampleCode))) as Success).data
            assertTrue(engine11.semantic(statements).isOk())

            // interpret
            assertTrue(engine11.interpret(statements).isOk())

            val decl = Declaration("x", "number", NumberLiteral(42.0))
            assertTrue(engine11.interpret(listOf(decl)).isOk())
        }
    }

    @Nested
    inner class StatementRulesCornerCasesTests {
        @Test
        fun `const declaration without initializer fails in parser rule`() {
            val invalidConstCode = "const x: number;"
            val res = engine11.parse(engine11.lex(StringContent(invalidConstCode)))
            assertTrue(res is Failure<*>)
            assertTrue(res.msg.contains("const declaration 'x' must have an initializer"))
        }

        @Test
        fun `v1_0 rejects const keyword during lexing or parsing`() {
            val constCode = "const x: number = 10;"
            val res = engine10.parse(engine10.lex(StringContent(constCode)))
            // In v1.0, const is not a valid keyword declaration
            assertTrue(res is Failure<*>)
        }
    }
}
