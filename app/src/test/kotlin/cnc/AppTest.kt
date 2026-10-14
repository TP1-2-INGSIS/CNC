package cnc

import cnc.cli.args.ArgsContainer
import cnc.common.Failure
import cnc.common.Success
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.io.ByteArrayInputStream
import kotlin.io.path.createTempFile

class AppTest {

    private fun args(
        positionals: List<String> = emptyList(),
        options: Map<String, String> = emptyMap(),
        flags: Set<String> = emptySet()
    ): ArgsContainer {
        val container = ArgsContainer()
        positionals.forEach { container.addPositional(it) }
        options.forEach { (k, v) -> container.addOption(k, v) }
        flags.forEach { container.addFlag(it) }
        return container
    }

    @Nested
    inner class RunCommandTests {

        @Test
        fun `fails when file parameter is missing`() {
            val res = RunCommand.execute(args())
            assertTrue(res is Failure<*>)
            assertTrue(res.msg.contains("Missing required source file"))
        }

        @Test
        fun `executes valid file successfully`() {
            val tempFile = createTempFile("valid_run", ".prs").toFile()
            tempFile.deleteOnExit()
            tempFile.writeText("let x: number = 42;\nprintln(x);")

            val res = RunCommand.execute(args(positionals = listOf(tempFile.absolutePath)))
            assertTrue(res is Success)
        }

        @Test
        fun `fails when executing invalid syntax file`() {
            val tempFile = createTempFile("invalid_run", ".prs").toFile()
            tempFile.deleteOnExit()
            tempFile.writeText("let x: = ;")

            val res = RunCommand.execute(args(positionals = listOf(tempFile.absolutePath), options = mapOf("version" to "1.1")))
            assertTrue(res is Failure<*>)
        }
    }

    @Nested
    inner class ValidateCommandTests {

        @Test
        fun `fails when file parameter is missing`() {
            val res = ValidateCommand.execute(args())
            assertTrue(res is Failure<*>)
            assertTrue(res.msg.contains("Missing required source file"))
        }

        @Test
        fun `validates valid source file`() {
            val tempFile = createTempFile("valid_val", ".prs").toFile()
            tempFile.deleteOnExit()
            tempFile.writeText("let s: string = \"hello\";")

            val res = ValidateCommand.execute(args(positionals = listOf(tempFile.absolutePath), options = mapOf("version" to "1.1")))
            assertTrue(res is Success)
        }

        @Test
        fun `fails validation for invalid source file`() {
            val tempFile = createTempFile("invalid_val", ".prs").toFile()
            tempFile.deleteOnExit()
            tempFile.writeText("let s: string = 123;")

            val res = ValidateCommand.execute(args(positionals = listOf(tempFile.absolutePath)))
            assertTrue(res is Failure<*>)
        }
    }

    @Nested
    inner class FormatCommandTests {

        @Test
        fun `fails when file parameter is missing`() {
            val res = FormatCommand.execute(args())
            assertTrue(res is Failure<*>)
        }

        @Test
        fun `formats source file without custom config`() {
            val tempFile = createTempFile("valid_fmt", ".prs").toFile()
            tempFile.deleteOnExit()
            tempFile.writeText("let x:number=1;")

            val res = FormatCommand.execute(args(options = mapOf("file" to tempFile.absolutePath)))
            assertTrue(res is Success)
        }

        @Test
        fun `formats source file with custom config`() {
            val tempFile = createTempFile("valid_fmt_cfg", ".prs").toFile()
            tempFile.deleteOnExit()
            tempFile.writeText("let x:number=1;")

            val cfgFile = createTempFile("config", ".json").toFile()
            cfgFile.deleteOnExit()
            cfgFile.writeText("""{"space-before-colon": true}""")

            val res = FormatCommand.execute(args(
                positionals = listOf(tempFile.absolutePath),
                options = mapOf("config" to cfgFile.absolutePath)
            ))
            assertTrue(res is Success)
        }
    }

    @Nested
    inner class GccCommandTests {

        @Test
        fun `fails when file parameter is missing`() {
            val res = GccCommand.execute(args())
            assertTrue(res is Failure<*>)
        }

        @Test
        fun `executes with verbose and check flags`() {
            val res = GccCommand.execute(args(
                positionals = listOf("test.cnc"),
                flags = setOf("verbose", "check")
            ))
            assertTrue(res is Success)
            assertTrue(res.msg.contains("(verbose)"))
            assertTrue(res.msg.contains("(check-only)"))
        }
    }

    @Nested
    inner class HelpAttributesAndMainTests {

        @Test
        fun `help attributes contain expected documentation`() {
            assertNotNull(RunWithHelp.getHelpText())
            assertNotNull(ValidateWithHelp.getHelpText())
            assertNotNull(FormatWithHelp.getHelpText())
            assertNotNull(GCNCWithHelp.getHelpText())
        }

        @Test
        fun `CLISystem contains all registered commands`() {
            assertTrue(CLISystem.cmds.containsKey("run"))
            assertTrue(CLISystem.cmds.containsKey("validate"))
            assertTrue(CLISystem.cmds.containsKey("format"))
            assertTrue(CLISystem.cmds.containsKey("gcnc"))
        }

        @Test
        fun `main execution terminates cleanly on exit input`() {
            val originalIn = System.`in`
            try {
                System.setIn(ByteArrayInputStream("exit\n".toByteArray()))
                main()
            } finally {
                System.setIn(originalIn)
            }
        }
    }
}
