package cnc.cli

import cnc.cli.args.ArgsManager
import cnc.cli.command.Command
import cnc.cli.command.HelpAttribute
import cnc.common.ErrorType
import cnc.common.Failure
import cnc.common.Result
import cnc.common.Success
import cnc.common.flatMap
import cnc.common.map
import cnc.common.onFailure
import cnc.common.onSuccess

data class CommandSystem(
    val cmds: Map<String, Command>,
    val io: IOManager = StdIO()
) {
    private fun readCommandFromInput(): Result<String> {
        io.write("-> $ ")
        return try {
            Success("Read input correctly", io.read())
        } catch (e: Exception) {
            Failure("Failed to read input: ${e.message}", ErrorType.CLI)
        }
    }

    private fun getGlobalHelp(argTokens: List<String>): String {
        if (argTokens.isNotEmpty()) {
            val targetCmd = argTokens[0]
            val cmd = cmds[targetCmd]
                ?: return "Cannot show help for '$targetCmd': command not found."

            if (cmd is HelpAttribute) {
                return cmd.getHelpText()
            }

            return "Command: ${cmd.tag}\nNo detailed documentation available."
        }

        val sb = StringBuilder()
        sb.appendLine("Available commands:")
        sb.appendLine("  ${"help [command]".padEnd(20)} Display general help or help for a specific command")
        sb.appendLine("  ${"exit / quit".padEnd(20)} Exit the CLI session")

        cmds.values.distinctBy { it.tag }.forEach { cmd ->
            val description = if (cmd is HelpAttribute) cmd.description else "(no description)"
            sb.appendLine("  ${cmd.tag.padEnd(20)} $description")
        }

        return sb.toString().trimEnd()
    }

    private fun executeLine(line: String): Result<Boolean> {
        val trimmed = line.trim()
        if (trimmed.isBlank()) {
            return Success("Alert: No command provided. Type 'help' for available commands.", true)
        }

        val tokens = ArgsManager.tokenize(trimmed)
        if (tokens.isEmpty()) {
            return Success("Alert: No command entered.", true)
        }

        val commandTag = tokens[0]
        val argTokens = tokens.drop(1)

        if (commandTag == "exit" || commandTag == "quit") {
            return Success("Exiting CLI session...", false)
        }

        if (commandTag == "help") {
            return Success(getGlobalHelp(argTokens), true)
        }

        val cmd = cmds[commandTag]
            ?: return Failure("Command '$commandTag' is not registered. Type 'help' for available commands.", ErrorType.CLI)

        val args = ArgsManager.getArgsContainer(argTokens)
        val result = cmd.execute(args)

        return result.map { true }
    }

    fun run(): Result<Unit> {
        while (true) {
            val shouldContinue = readCommandFromInput()
                .flatMap { line -> executeLine(line) }
                .onSuccess { res ->
                    if (res.msg.isNotBlank()) io.writeLine(res.msg)
                }
                .onFailure { err ->
                    io.writeLine("Command error: ${err.msg}")
                }

            if (shouldContinue is Success && !shouldContinue.data) {
                break
            }
        }

        return Success("Command system finished correctly!", Unit)
    }
}
