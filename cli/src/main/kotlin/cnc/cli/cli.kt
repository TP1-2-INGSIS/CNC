package cnc.cli

import cnc.common.Result
import cnc.common.Success
import cnc.common.Failure
import cnc.common.ErrorType

data class CommandSystem(
  val io: IOManager,

) {
    private fun readCommandFromInput() : Result<String> {
      ioManager.write("-> $ ");
      val input: String = ioManager.read();
      return Success<>("Red input correctly", input);
    }

    fun run() {
        String input = "";
        while(running) {

            when (val result = readCommandFromInput()) {
                is Success -> input = resutl.data()
                is Failure -> Failure(ErrorType.CLI, "There was an unexpected error! \n\t" + result.msg())
            }

            if (input.equals("exit")) break;

            Command cmd = handleOperationResult(CommandParser.getCommand(input));

            if (cmd == null) return new OperationError("The command provided was not expected", 101);

            commandContext = new CommandContext(
                    ArgsManager.getArgsContainer(input.split(" ")),
                    "user",
                    pwd,
                    root
            );

            when (val result = cmd.execute(commandContext)) {
                is Success -> ioManager.write(s.msg() + "\n")
                is Failure -> ioManager.write("command failed with msg: \n\t" + result.msg() + "\n");
            }
        }
        return new OperationSuccess<>("Command system finished correctly!", null);
    }
}
