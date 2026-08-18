package cnc.cli.command

import cnc.cli.args.ArgsContainer
import cnc.cli.args.Param
import cnc.cli.args.ParamType

import cnc.common.Result
import cnc.common.Success
import cnc.common.Failure

// como el dominio de mis comandos va a ser
// muy chico, porque solo quiero que funcionen
// el gcc y el run/exec, no hace falta que execute
// reciba ningun tipo de contexto
interface Command {
  val tag: String
  fun execute(params: ArgsContainer) : Result<Unit>
}

// gcc man.cnc -> man.exe -> interpreter man.cnc -> execute
// gcc man.cnc -> execute
object GccCommand : Command { 
  override val tag = "gcnc";

  // lo hago optional para saber que ruta es
  // gcnc --file=/home/main.cnc
  val params = object {
    val flags = setOf(
      Param("--out",      "binary file compiled name", ParamType.FLAG),
      Param("--check",    "check if the file is type safe", ParamType.FLAG),
      Param("--verbose",  "Display all the process messages", ParamType.FLAG)
    )
    val optional = listOf(
      Param("--file",     "file/path to be compiled", ParamType.OPTIONAL),
    )
    val positional = listOf<Param>()
  }

  override fun execute(params: ArgsContainer) : Result<Unit> = TODO("not implemented yet!")
}
