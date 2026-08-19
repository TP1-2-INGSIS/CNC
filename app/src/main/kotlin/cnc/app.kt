package cnc
 
import cnc.cli.CommandSystem
import cnc.cli.StdIO
import cnc.cli.command.CommandProvider

fun main() {
    val provider = CommandProvider()
    val sys = CommandSystem(provider.getAll(), StdIO())
    sys.run()
}

