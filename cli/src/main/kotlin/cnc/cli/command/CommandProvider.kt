package cnc.cli.command

import cnc.common.ErrorType
import cnc.common.Failure
import cnc.common.Provider
import cnc.common.Result
import cnc.common.Success

class CommandProvider(
  private val commands: MutableMap<String, Command> = mutableMapOf(),
) : Provider<String, Command> {
  init {
    registerDefaults()
  }

  private fun registerDefaults() {
    if (commands.isNotEmpty()) return

    register(
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
      ),
    )
  }

  fun register(command: Command): CommandProvider {
    commands[command.tag] = command
    return this
  }

  override fun getTypes(): Set<String> = commands.keys

  override fun getValue(type: String): Command? = commands[type]

  fun getCommand(command: String): Result<Command> {
    val cmd =
      commands[command]
        ?: return Failure("Command '$command' is not registered.", ErrorType.CLI)
    return Success("Command found", cmd)
  }

  fun getAll(): Map<String, Command> = commands.toMap()
}
