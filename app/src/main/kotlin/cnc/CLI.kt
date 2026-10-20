package cnc

import cnc.cli.CommandSystem
import cnc.cli.args.ArgsContainer
import cnc.cli.command.Command
import cnc.cli.command.HelpAttribute
import cnc.common.ErrorType
import cnc.common.Failure
import cnc.common.Result
import cnc.common.Success
import cnc.printscript.PsVersioner
import java.io.File

object RunCommand : Command {
    override val tag = "run"

    override fun execute(params: ArgsContainer): Result<Unit> {
        val file =
            params.getOption("file") ?: params.getPositional(0)
                ?: return Failure("Missing required source file. Usage: run <file>", ErrorType.CLI)

        val version = params.getOption("version") ?: "1.1"
        val ps = PsVersioner.version(version)
        val result = ps.execute(File(file))
        return when (result) {
            is Failure -> {
                println("ERROR: ${result.msg}")
                Failure(result.msg, result.type)
            }
            is Success -> Success("Execution finished successfully", Unit)
        }
    }
}

val RunWithHelp =
    HelpAttribute(
        wrapped = RunCommand,
        description = "Executes a PrintScript source file",
        usage = "run <file>",
        paramHelp =
            mapOf(
                "<file>" to "Source file path to run",
            ),
    )

object ValidateCommand : Command {
    override val tag = "validate"

    override fun execute(params: ArgsContainer): Result<Unit> {
        val file =
            params.getOption("file") ?: params.getPositional(0)
                ?: return Failure("Missing required source file. Usage: validate <file>", ErrorType.CLI)

        val version = params.getOption("version") ?: "1.1"
        val ps = PsVersioner.version(version)
        val result = ps.validate(File(file))
        return when (result) {
            is Failure -> {
                println("VALIDATION ERROR: ${result.msg}")
                Failure(result.msg, result.type)
            }
            is Success -> {
                println("VALID: '$file' is valid PrintScript")
                Success("Valid PrintScript", Unit)
            }
        }
    }
}

val ValidateWithHelp =
    HelpAttribute(
        wrapped = ValidateCommand,
        description = "Validates syntax and types of a PrintScript source file",
        usage = "validate <file>",
        paramHelp =
            mapOf(
                "<file>" to "Source file path to validate",
            ),
    )

object FormatCommand : Command {
    override val tag = "format"

    override fun execute(params: ArgsContainer): Result<Unit> {
        val file =
            params.getOption("file") ?: params.getPositional(0)
                ?: return Failure("Missing required source file. Usage: format <file> [--config=<path>]", ErrorType.CLI)

        val targetFile = File(file)
        if (!targetFile.exists()) {
            return Failure("File not found: '$file'", ErrorType.CLI)
        }

        val configFile = params.getOption("config")
        val configJson = configFile?.let { File(it).readText() }

        return runCatching {
            val ps = PsVersioner.version("1.1")
            val formatted = ps.format(targetFile, configJson)
            targetFile.writeText(formatted)
            formatted
        }.fold(
            onSuccess = { Success("Formatted '$file' successfully", Unit) },
            onFailure = { e -> Failure(e.message ?: "Formatting failed", ErrorType.CLI) },
        )
    }
}

val FormatWithHelp =
    HelpAttribute(
        wrapped = FormatCommand,
        description = "Formats a PrintScript source file to canonical style",
        usage = "format --file=<path> [--config=<path>]",
        paramHelp =
            mapOf(
                "--file=<path>" to "Source file path to format",
                "--config=<path>" to "Formatting configuration JSON file",
            ),
    )

object LintCommand : Command {
    override val tag = "lint"

    override fun execute(params: ArgsContainer): Result<Unit> {
        val file =
            params.getOption("file") ?: params.getPositional(0)
                ?: return Failure("Missing required source file. Usage: lint <file> [--config=<path>]", ErrorType.CLI)

        val version = params.getOption("version") ?: "1.1"
        val configFile = params.getOption("config")
        val configJson = configFile?.let { File(it).readText() } ?: "{}"

        return runCatching {
            val ps = PsVersioner.version(version)
            ps.lint(File(file), configJson)
        }.fold(
            onSuccess = { warnings ->
                if (warnings.isEmpty()) {
                    Success("No lint warnings found.", Unit)
                } else {
                    Failure("Lint warnings:\n" + warnings.joinToString("\n"), ErrorType.CLI)
                }
            },
            onFailure = { e -> Failure(e.message ?: "Linting failed", ErrorType.CLI) },
        )
    }
}

val LintWithHelp =
    HelpAttribute(
        wrapped = LintCommand,
        description = "Lints a PrintScript source file for style and conventions",
        usage = "lint <file> [--config=<path>] [--version=<version>]",
        paramHelp =
            mapOf(
                "<file>" to "Source file path to lint",
                "--config=<path>" to "Linting configuration JSON file",
                "--version=<version>" to "PrintScript language version (default: 1.1)",
            ),
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

        val details =
            buildString {
                if (isVerbose) append(" (verbose)")
                if (isCheck) append(" (check-only)")
            }

        return Success("Compiling '$file'$details...", Unit)
    }
}

val GCNCWithHelp =
    HelpAttribute(
        wrapped = GccCommand,
        description = "Compiles CNC source files",
        usage = "gcnc --file=<path> [--verbose] [--check]",
        paramHelp =
            mapOf(
                "--file=<path>" to "Source file path to compile",
                "--verbose" to "Display detailed compiler output",
                "--check" to "Syntax and type checking only",
            ),
    )

val CLISystem =
    CommandSystem(
        mapOf(
            RunCommand.tag to RunWithHelp,
            ValidateCommand.tag to ValidateWithHelp,
            GccCommand.tag to GCNCWithHelp,
            FormatCommand.tag to FormatWithHelp,
            LintCommand.tag to LintWithHelp,
        ),
    )
