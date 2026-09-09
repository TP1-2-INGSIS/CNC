package cnc.config

import cnc.cli.args.ArgsContainer
import cnc.cli.command.Command
import cnc.cli.command.HelpAttribute

import cnc.common.ErrorType
import cnc.common.Failure
import cnc.common.FileContent
import cnc.common.Result
import cnc.common.Success

import cnc.parser.Parser

/** Parser de PrintScript reutilizando la config de gramáticas existente. */
val printScriptParser = Parser(grammars, terminators)

/**
 * Comando CLI `format` — formatea un archivo PrintScript y devuelve el texto
 * canónico. Comando independiente del `run`/compile (Decisión 2): formatear no
 * altera la ejecución, es una herramienta de flujo de trabajo.
 *
 * Pipeline: FileContent -> Lexer -> Parser -> AST -> Formatter -> String.
 * Reutiliza la config existente (`printScriptLexer`, `grammars`) y la config del
 * formatter (`printScriptFormatter`).
 */
object FormatCommand : Command {
    override val tag = "format"

    override fun execute(params: ArgsContainer): Result<Unit> {
        val file = params.getOption("file") ?: params.getPositional(0)
            ?: return Failure(
                "Missing required source file. Usage: format --file=<path>",
                ErrorType.CLI
            )

        return runCatching {
            val content = FileContent(file)
            val tokens = printScriptLexer.tokenize(content)
            val statements = printScriptParser.getASTs(tokens).toList()
            printScriptFormatter.format(statements)
        }.fold(
            onSuccess = { formatted -> Success(formatted, Unit) },
            onFailure = { e -> Failure(e.message ?: "Formatting failed", ErrorType.CLI) }
        )
    }
}

val FormatWithHelp = HelpAttribute(
    wrapped = FormatCommand,
    description = "Formats a PrintScript source file to canonical style",
    usage = "format --file=<path>",
    paramHelp = mapOf(
        "--file=<path>" to "Source file path to format"
    )
)
