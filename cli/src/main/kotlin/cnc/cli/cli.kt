package cnc.cli

import cnc.common.Result
import cnc.common.Success
import cnc.common.Failure
import cnc.common.ErrorType

import cnc.cli.command.Command
import cnc.cli.args.ArgsManager

data class CommandSystem(
  val cmds: Map<String, Command>,
  val io: IOManager = StdIO()
) {
    private fun readCommandFromInput() : Result<String> {
      io.write("-> $ ");
      val input: String = io.read();
      return Success("Red input correctly", input);
    }

    fun run() : Result<Unit> {
        var input: String = "";

        while(true) {

            when (val result = readCommandFromInput()) {
                is Success -> input = result.data
                is Failure -> Failure<Unit>("There was an unexpected error! \n\t" + result.msg, ErrorType.CLI)
            }

            if (input.equals("exit")) break;

            if (input !in cmds.keys) {
              io.write("The command provided is not registered!");
              continue;
            }

            val cmd = cmds[input]!!;
            val args = ArgsManager.getArgsContainer(input.split(" "))

            // Lo saque pero, cada comando podria tener un contexto
            // lo cual nos permitira hacer que le podamos pasar
            // args al programa compilado, tales como hace cpp
            when (val result = cmd.execute(args)) {
                is Success -> io.write(result.msg + "\n")
                is Failure -> io.write("Command failed with msg: \n\t" + result.msg + "\n");
            }
        }

        return Success("Command system finished correctly!", Unit);
    }
}
