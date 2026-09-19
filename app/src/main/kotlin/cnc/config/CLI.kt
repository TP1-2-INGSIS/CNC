package cnc.config

import cnc.Compiler
import cnc.common.ErrorType
import cnc.common.Failure
import cnc.common.FileContent
import cnc.common.Result
import cnc.common.Success

import cnc.cli.args.ArgsContainer
import cnc.cli.CommandSystem
import cnc.cli.command.Command
import cnc.cli.command.HelpAttribute

object RunCommand : Command {
    override val tag = "run"

    override fun execute(params: ArgsContainer): Result<Unit> {
        val file = params.getOption("file") ?: params.getPositional(0)
            ?: return Failure<Unit>("Missing required source file. Usage: run <file>", ErrorType.CLI)

        val config = ConfigFactory.create()
        val compiler = Compiler(config)
        val result = compiler.execute(FileContent(file))
        return when (result) {
            is Failure -> {
                println("ERROR: ${result.msg}")
                Failure<Unit>(result.msg, result.type)
            }
            is Success -> Success("Execution finished successfully", Unit)
        }
    }
}

val RunWithHelp = HelpAttribute(
    wrapped = RunCommand,
    description = "Executes a PrintScript source file",
    usage = "run <file>",
    paramHelp = mapOf(
        "<file>" to "Source file path to run"
    )
)

object ValidateCommand : Command {
    override val tag = "validate"

    override fun execute(params: ArgsContainer): Result<Unit> {
        val file = params.getOption("file") ?: params.getPositional(0)
            ?: return Failure<Unit>("Missing required source file. Usage: validate <file>", ErrorType.CLI)

        val config = ConfigFactory.create()
        val compiler = Compiler(config)
        val result = compiler.validate(FileContent(file))
        return when (result) {
            is Failure -> {
                println("VALIDATION ERROR: ${result.msg}")
                Failure<Unit>(result.msg, result.type)
            }
            is Success -> {
                println("VALID: '$file' is valid PrintScript")
                Success("Valid PrintScript", Unit)
            }
        }
    }
}

val ValidateWithHelp = HelpAttribute(
    wrapped = ValidateCommand,
    description = "Validates syntax and types of a PrintScript source file",
    usage = "validate <file>",
    paramHelp = mapOf(
        "<file>" to "Source file path to validate"
    )
)

object GccCommand : Command {
    override val tag = "gcnc"

    override fun execute(params: ArgsContainer): Result<Unit> {
        val file = params.getOption("file") ?: params.getPositional(0)
        if (file == null) {
            return Failure("Missing required source file. Usage: gcnc --file=<path>", ErrorType.CLI)
        }

        val isVerbose = params.hasFlag("verbose")
        val isCheck = params.hasFlag("check")

        val details = buildString {
            if (isVerbose) append(" (verbose)")
            if (isCheck) append(" (check-only)")
        }

        return Success("Compiling '$file'$details...", Unit)
    }
}

val GCNCWithHelp = HelpAttribute(
    wrapped = GccCommand,
    description = "Compiles CNC source files",
    usage = "gcnc --file=<path> [--verbose] [--check]",
    paramHelp = mapOf(
        "--file=<path>" to "Source file path to compile",
        "--verbose" to "Display detailed compiler output",
        "--check" to "Syntax and type checking only"
    )
)

val CLISystem = CommandSystem(mapOf(
  RunCommand.tag to RunWithHelp,
  ValidateCommand.tag to ValidateWithHelp,
  GccCommand.tag to GCNCWithHelp,
  FormatCommand.tag to FormatWithHelp
))
