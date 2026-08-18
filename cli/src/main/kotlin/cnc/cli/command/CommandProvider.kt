package cnc.cli.command

import cnc.common.Result
import cnc.common.Success
import cnc.common.Failure
import cnc.common.ErrorType

class CommandProvider {
    val commands = mapOf<String, Command>(
      "gcc" to GccCommand
    );

    fun getCommandsName() : Set<String> = commands.keys

    fun getCommand(command: String) : Result<Command> {
        if (command !in getCommandsName()) return Failure("The command provided is not registered!", ErrorType.CLI);
        // no es lo mejor esto, pero bueno, por ahora solo quiero que ande
        return Success("The command was found!", commands[command]!!);
    }
}
