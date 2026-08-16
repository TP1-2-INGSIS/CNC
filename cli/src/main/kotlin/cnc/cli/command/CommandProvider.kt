package cnc.cli.command

class CommandProvider {
    val commandTypes = mapOf(
      "gcc" to GccCommand
    );

    fun getCommands() : Set<String> = commandTypes.keys

    private fun validateCommand(cmd: String) : Boolean = cmd in getCommands()

    fun getCommand(command: String) : Result<Command> {
        if (!validateCommand(command)) return Failure(ErrorType.CLI, "The command provided is not registered!");
        // no es lo mejor esto, pero bueno, por ahora solo quiero que ande
        return Success(commandTypes[command], "The command was found!");
    }
}
