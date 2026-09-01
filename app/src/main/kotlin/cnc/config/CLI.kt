package cnc.config

import cnc.common.Result
import cnc.common.Failure
import cnc.common.Success
import cnc.common.ErrorType

import cnc.cli.args.ArgsContainer
import cnc.cli.CommandSystem
import cnc.cli.command.Command
import cnc.cli.command.HelpAttribute

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
  GccCommand.tag to GCNCWithHelp
))
