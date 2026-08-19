package cnc.cli

import cnc.cli.command.Command
import cnc.cli.command.GccCommand
import cnc.cli.command.HelpAttribute
import cnc.common.ErrorType
import cnc.common.Failure
import cnc.common.Success
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class CommandSystemTest {

    @Test
    fun `test command execution in CommandSystem`() {
        val testIO = TestIO()
        testIO.addInput("gcnc --file=main.cnc --verbose")
        testIO.addInput("exit")

        val sys = CommandSystem(
            mapOf("gcnc" to GccCommand),
            testIO
        )
        val result = sys.run()

        assertTrue(result is Success)
        val output = testIO.getOutput()
        assertTrue(output.contains("Compiling 'main.cnc' (verbose)..."))
    }

    @Test
    fun `test HelpAttribute decorator handles --help flag`() {
        val testIO = TestIO()
        testIO.addInput("gcnc --help")
        testIO.addInput("exit")

        val decorated = HelpAttribute(
            wrapped = GccCommand,
            description = "Compiles CNC files",
            usage = "gcnc --file=<path> [--verbose]",
            paramHelp = mapOf(
                "--file=<path>" to "Source file",
                "--verbose" to "Verbose mode"
            )
        )

        val sys = CommandSystem(
            mapOf("gcnc" to decorated),
            testIO
        )
        sys.run()

        val output = testIO.getOutput()
        assertTrue(output.contains("Command: gcnc"))
        assertTrue(output.contains("Description: Compiles CNC files"))
        assertTrue(output.contains("--file=<path>"))
    }

    @Test
    fun `test global help command displays registered commands`() {
        val testIO = TestIO()
        testIO.addInput("help")
        testIO.addInput("exit")

        val decorated = HelpAttribute(
            wrapped = GccCommand,
            description = "Compiles CNC files"
        )

        val sys = CommandSystem(
            mapOf("gcnc" to decorated),
            testIO
        )
        sys.run()

        val output = testIO.getOutput()
        assertTrue(output.contains("Available commands:"))
        assertTrue(output.contains("gcnc"))
        assertTrue(output.contains("Compiles CNC files"))
    }

    @Test
    fun `test specific help command`() {
        val testIO = TestIO()
        testIO.addInput("help gcnc")
        testIO.addInput("exit")

        val decorated = HelpAttribute(
            wrapped = GccCommand,
            description = "Compiles CNC files"
        )

        val sys = CommandSystem(
            mapOf("gcnc" to decorated),
            testIO
        )
        sys.run()

        val output = testIO.getOutput()
        assertTrue(output.contains("Command: gcnc"))
        assertTrue(output.contains("Description: Compiles CNC files"))
    }

    @Test
    fun `test unknown command shows helpful message`() {
        val testIO = TestIO()
        testIO.addInput("unknown_cmd")
        testIO.addInput("exit")

        val sys = CommandSystem(
            mapOf("gcnc" to GccCommand),
            testIO
        )
        sys.run()

        val output = testIO.getOutput()
        assertTrue(output.contains("Command 'unknown_cmd' is not registered"))
    }

    @Test
    fun `test command returning Failure displays error`() {
        val testIO = TestIO()
        testIO.addInput("fail_cmd")
        testIO.addInput("exit")

        val failingCmd = object : Command {
            override val tag = "fail_cmd"
            override fun execute(params: cnc.cli.args.ArgsContainer) =
                Failure<Unit>("Something went wrong", ErrorType.CLI)
        }

        val sys = CommandSystem(
            mapOf("fail_cmd" to failingCmd),
            testIO
        )
        sys.run()

        val output = testIO.getOutput()
        assertTrue(output.contains("Command error: Something went wrong"))
    }
}
