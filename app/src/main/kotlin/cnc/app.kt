package cnc

import cnc.cli.CommandSystem
import cnc.cli.command.Command
import cnc.cli.StdIO
import cnc.cli.command.CommandProvider
import cnc.cli.command.GccCommand

fun main() {
  val sys = CommandSystem(mapOf<String, Command>(
      "gcc" to GccCommand
    ), StdIO())
  sys.run()
}
