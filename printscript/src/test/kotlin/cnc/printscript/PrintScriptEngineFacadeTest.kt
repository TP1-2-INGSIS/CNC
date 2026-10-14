package cnc.printscript

import cnc.ast.Declaration
import cnc.ast.NumberLiteral
import cnc.common.FileContent
import cnc.common.InputStreamContent
import cnc.common.StringContent
import cnc.common.Failure
import cnc.common.Success
import cnc.printscript.profile.PrintScriptProfileRegistry
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.io.File
import kotlin.io.path.createTempFile

class PrintScriptEngineFacadeTest {

    private val engine10: PrintScriptFacade = PsVersioner.version("1.0")
    private val engine11: PrintScriptFacade = PsVersioner.version("1.1")

    @Nested
    inner class VersionAndRegistryTests {

        @Test
        fun `resolves versions correctly and throws on invalid`() {
            assertEquals(LanguageVersion.V1_0, LanguageVersion.from("1.0"))
            assertEquals(LanguageVersion.V1_1, LanguageVersion.from("1.1"))

            assertThrows(UnsupportedVersionException::class.java) {
                LanguageVersion.from("9.9")
            }
        }

        @Test
        fun `engine factory methods for versions`() {
            val e1 = PrintScriptEngine(LanguageVersion.V1_0)
            val e2 = PrintScriptEngine(LanguageVersion.V1_1)
            assertNotNull(e1)
            assertNotNull(e2)

            val p1 = PrintScriptProfileRegistry.forVersion(LanguageVersion.V1_0)
            val p2 = PrintScriptProfileRegistry.forVersion(LanguageVersion.V1_1)
            assertNotNull(p1)
            assertNotNull(p2)
        }
    }

    @Nested
    inner class FacadeOverloadsTests {

        private val sampleCode = "let x: number = 42;\nprintln(x);"

        @Test
        fun `execute overloads with File, InputStream, ContentManager, and custom IO`() {
            val outputList = mutableListOf<String>()
            val resStr = engine11.execute(
                source = sampleCode,
                input = { _: String -> "input" },
                envProvider = { _: String -> null },
                output = { str: String -> outputList.add(str) }
            )
            assertTrue(resStr.isOk())
            assertEquals(listOf("42"), outputList)

            val tempFile = createTempFile("ps_exec", ".prs").toFile()
            tempFile.deleteOnExit()
            tempFile.writeText(sampleCode)

            assertTrue(engine11.execute(tempFile).isOk())
            assertTrue(engine11.execute(sampleCode.byteInputStream()).isOk())
            assertTrue(engine11.execute(StringContent(sampleCode)).isOk())
            assertTrue(engine11.execute(FileContent(tempFile.absolutePath)).isOk())
            assertTrue(engine11.execute(InputStreamContent(sampleCode.byteInputStream())).isOk())
        }

        @Test
        fun `validate overloads with all input types`() {
            val tempFile = createTempFile("ps_val", ".prs").toFile()
            tempFile.deleteOnExit()
            tempFile.writeText(sampleCode)

            assertTrue(engine11.validate(sampleCode).isOk())
            assertTrue(engine11.validate(tempFile).isOk())
            assertTrue(engine11.validate(sampleCode.byteInputStream()).isOk())
            assertTrue(engine11.validate(StringContent(sampleCode)).isOk())

            val decl = Declaration("x", "number", NumberLiteral(42.0))
            assertTrue(engine11.validate(listOf(decl)).isOk())
        }

        @Test
        fun `format overloads with all input types and statements`() {
            val raw = "let x:number=42;"
            val tempFile = createTempFile("ps_fmt", ".prs").toFile()
            tempFile.deleteOnExit()
            tempFile.writeText(raw)

            val expected = "let x: number = 42;"
            assertEquals(expected, engine11.format(raw))
            assertEquals(expected, engine11.format(tempFile))
            assertEquals(expected, engine11.format(raw.byteInputStream()))
            assertEquals(expected, engine11.format(StringContent(raw)))

            val decl = Declaration("x", "number", NumberLiteral(42.0))
            assertEquals(expected, engine11.format(listOf(decl)))
        }

        @Test
        fun `lint overloads with config string and config file`() {
            val raw = "let x: number = 42;"
            val tempFile = createTempFile("ps_lint", ".prs").toFile()
            tempFile.deleteOnExit()
            tempFile.writeText(raw)

            val cfgFile = createTempFile("lint_cfg", ".json").toFile()
            cfgFile.deleteOnExit()
            cfgFile.writeText("{}")

            assertEquals(emptyList<String>(), engine11.lint(raw, "{}"))
            assertEquals(emptyList<String>(), engine11.lint(tempFile, "{}"))
            assertEquals(emptyList<String>(), engine11.lint(raw.byteInputStream(), "{}"))
            assertEquals(emptyList<String>(), engine11.lint(StringContent(raw), "{}"))
            assertEquals(emptyList<String>(), engine11.lint(listOf(Declaration("x", "number", NumberLiteral(42.0))), "{}"))

            assertEquals(emptyList<String>(), engine11.lint(raw, cfgFile))
            assertEquals(emptyList<String>(), engine11.lint(tempFile, cfgFile))
            assertEquals(emptyList<String>(), engine11.lint(raw.byteInputStream(), cfgFile))
            assertEquals(emptyList<String>(), engine11.lint(listOf(Declaration("x", "number", NumberLiteral(42.0))), cfgFile))
        }

        @Test
        fun `lex, parse, semantic, and interpret pipeline stages`() {
            val tempFile = createTempFile("ps_stages", ".prs").toFile()
            tempFile.deleteOnExit()
            tempFile.writeText(sampleCode)

            // lex
            assertTrue(engine11.lex(sampleCode).toList().isNotEmpty())
            assertTrue(engine11.lex(tempFile).toList().isNotEmpty())
            assertTrue(engine11.lex(sampleCode.byteInputStream()).toList().isNotEmpty())
            assertTrue(engine11.lex(StringContent(sampleCode)).toList().isNotEmpty())

            // parse
            assertTrue(engine11.parse(sampleCode).isOk())
            assertTrue(engine11.parse(tempFile).isOk())
            assertTrue(engine11.parse(sampleCode.byteInputStream()).isOk())
            assertTrue(engine11.parse(StringContent(sampleCode)).isOk())

            // semantic
            assertTrue(engine11.semantic(sampleCode).isOk())
            assertTrue(engine11.semantic(tempFile).isOk())
            assertTrue(engine11.semantic(sampleCode.byteInputStream()).isOk())
            assertTrue(engine11.semantic(StringContent(sampleCode)).isOk())

            // interpret
            assertTrue(engine11.interpret(sampleCode).isOk())
            assertTrue(engine11.interpret(tempFile).isOk())
            assertTrue(engine11.interpret(sampleCode.byteInputStream()).isOk())
            assertTrue(engine11.interpret(StringContent(sampleCode)).isOk())

            val decl = Declaration("x", "number", NumberLiteral(42.0))
            assertTrue(engine11.interpret(listOf(decl)).isOk())
        }
    }

    @Nested
    inner class StatementRulesCornerCasesTests {

        @Test
        fun `const declaration without initializer fails in parser rule`() {
            val invalidConstCode = "const x: number;"
            val res = engine11.parse(invalidConstCode)
            assertTrue(res is Failure<*>)
            assertTrue(res.msg.contains("const declaration 'x' must have an initializer"))
        }

        @Test
        fun `v1_0 rejects const keyword during lexing or parsing`() {
            val constCode = "const x: number = 10;"
            val res = engine10.parse(constCode)
            // In v1.0, const is not a valid keyword declaration
            assertTrue(res is Failure<*>)
        }
    }
}
