package cnc.cli.command

// vamos a usar la notacion de -- para los nombres
// porque? porque esta facha
data class Param (
  val name: String,
  val description: String
)

// como el dominio de mis comandos va a ser
// muy chico, porque solo quiero que funcionen
// el gcc y el run/exec, no hace falta que execute
// reciba ningun tipo de contexto
interface Command {
  val tag: String
  val params: List<Param>
  fun execute() : Result<Unit>
}

object GccCommand : Command { 
  override val tag = "gcc";
  override val params = listOf(
    Param("--file", "file/path to be compiled"),
    Param("--out", "binary file compiled name")
  )
  override val execute() : Result<Unit> = TODO("not implemented yet!")
}
