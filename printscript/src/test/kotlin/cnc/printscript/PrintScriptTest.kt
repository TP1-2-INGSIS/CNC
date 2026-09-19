package cnc.printscript

import java.io.File
import java.io.InputStream
import cnc.common.Failure
import cnc.common.Success
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class PrintScriptTest {

    private val PrintScript: PrintScriptFacade = PsVersioner.version("1.1")

    @Test
    fun `lex generates tokens from string, file, and inputStream`() {
        val code = "let x: number = 10;"
        val tokensFromString = PrintScript.lex(code).toList()
        assertTrue(tokensFromString.isNotEmpty())

        val tempFile = File.createTempFile("ps_lex_test", ".prs")
        tempFile.writeText(code)
        try {
            val tokensFromFile = PrintScript.lex(tempFile).toList()
            assertEquals(tokensFromString.size, tokensFromFile.size)

            val stream: InputStream = code.byteInputStream()
            val tokensFromStream = PrintScript.lex(stream).toList()
            assertEquals(tokensFromString.size, tokensFromStream.size)
        } finally {
            tempFile.delete()
        }
    }

    @Test
    fun `parse creates statements and handles syntax errors`() {
        val validCode = "let a: string = \"hello\";\nprintln(a);"
        val parseResult = PrintScript.parse(validCode)
        assertTrue(parseResult.isOk())
        val statements = (parseResult as Success).data
        assertEquals(2, statements.size)

        val invalidCode = "let 123 = ;"
        val errorResult = PrintScript.parse(invalidCode)
        assertFalse(errorResult.isOk())
        assertTrue(errorResult is Failure)
    }

    @Test
    fun `semantic analysis validates types and reports errors`() {
        val validCode = "let x: number = 5 + 3;\nconst y: boolean = true;"
        val semanticResult = PrintScript.semantic(validCode)
        assertTrue(semanticResult.isOk())

        val invalidCode = "let x: number = \"not a number\";"
        val invalidResult = PrintScript.semantic(invalidCode)
        assertFalse(invalidResult.isOk())

        val analyzeResult = PrintScript.analyze(validCode)
        assertTrue(analyzeResult.isOk())
    }

    @Test
    fun `validate checks syntax and semantics without execution`() {
        val validCode = "let x: number = 10;\nprintln(x);"
        val validResult = PrintScript.validate(validCode)
        assertTrue(validResult.isOk())

        val invalidCode = "let x: number = \"fail\";"
        val invalidResult = PrintScript.validate(invalidCode)
        assertFalse(invalidResult.isOk())

        val tempFile = File.createTempFile("ps_val_test", ".prs")
        tempFile.writeText(validCode)
        try {
            assertTrue(PrintScript.validate(tempFile).isOk())
            val stream = validCode.byteInputStream()
            assertTrue(PrintScript.validate(stream).isOk())
        } finally {
            tempFile.delete()
        }
    }

    @Test
    fun `execute runs code with custom output, input, and envProvider`() {
        val output = mutableListOf<String>()
        val code = """
            let name: string = readInput("Enter name:");
            let mode: string = readEnv("APP_MODE");
            println("Name is: " + name);
            println("Mode is: " + mode);
            const flag: boolean = true;
            if (flag) {
                println("Flag is true");
            }
        """.trimIndent()

        val result = PrintScript.execute(
            source = code,
            input = { "Alice" },
            envProvider = { if (it == "APP_MODE") "PROD" else null },
            output = { output.add(it) }
        )

        assertTrue(result.isOk())
        assertEquals(3, output.size)
        assertEquals("Name is: Alice", output[0])
        assertEquals("Mode is: PROD", output[1])
        assertEquals("Flag is true", output[2])
    }

    @Test
    fun `execute works with file and stream inputs`() {
        val output = mutableListOf<String>()
        val code = "let count: number = 1 + 2;\nprintln(count);"

        val tempFile = File.createTempFile("ps_exec_test", ".prs")
        tempFile.writeText(code)
        try {
            val fileResult = PrintScript.execute(
                file = tempFile,
                output = { output.add(it) }
            )
            assertTrue(fileResult.isOk())
            assertEquals(listOf("3"), output)

            output.clear()
            val streamResult = PrintScript.execute(
                stream = code.byteInputStream(),
                output = { output.add(it) }
            )
            assertTrue(streamResult.isOk())
            assertEquals(listOf("3"), output)
        } finally {
            tempFile.delete()
        }
    }

    @Test
    fun `format produces canonical string`() {
        val code = "let   a :number=5; println( a );"
        val formatted = PrintScript.format(code)
        assertTrue(formatted.contains("let a: number = 5;"))
        assertTrue(formatted.contains("println(a);"))
    }

    @Test
    fun `lint detects naming convention violations`() {
        val configJson = """
            {
                "naming-convention": "camelCase"
            }
        """.trimIndent()

        val invalidCode = "let bad_name: number = 10;"
        val warnings = PrintScript.lint(invalidCode, configJson)
        assertTrue(warnings.isNotEmpty())

        val validCode = "let goodName: number = 10;"
        val noWarnings = PrintScript.lint(validCode, configJson)
        assertTrue(noWarnings.isEmpty())

        val configFile = File.createTempFile("ps_lint_config", ".json")
        configFile.writeText(configJson)
        try {
            val fileWarnings = PrintScript.lint(invalidCode, configFile)
            assertTrue(fileWarnings.isNotEmpty())
        } finally {
            configFile.delete()
        }
    }

    @Test
    fun `interpret executes parsed statements`() {
        val output = mutableListOf<String>()
        val code = "let msg: string = \"interpreter test\";\nprintln(msg);"
        val parseResult = PrintScript.parse(code)
        assertTrue(parseResult.isOk())
        val statements = (parseResult as Success).data

        val result = PrintScript.interpret(
            statements = statements,
            output = { output.add(it) }
        )
        assertTrue(result.isOk())
        assertEquals(listOf("interpreter test"), output)
    }

    @Test
    fun `execute is lazy and executes statement by statement failing fast`() {
        val output = mutableListOf<String>()
        val code = """
            println("step 1 executed");
            let x: number = "type mismatch";
            println("step 2 should not execute");
        """.trimIndent()

        val result = PrintScript.execute(
            source = code,
            output = { output.add(it) }
        )

        assertFalse(result.isOk())
        assertTrue(result is Failure)
        assertEquals(listOf("step 1 executed"), output)
    }
}
