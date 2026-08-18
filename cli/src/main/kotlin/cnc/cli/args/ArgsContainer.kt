package cnc.cli.args

enum class ParamType {
  POSITIONAL,
  OPTIONAL,
  FLAG
}

data class Param (
  val name: String,
  val description: String,
  val type: ParamType
)

class ArgsContainer(
  val flags     :MutableSet<String> = mutableSetOf(),
  val options   :MutableList<String> = mutableListOf(),
  val positional:MutableList<String> = mutableListOf()
) {

    fun addFlag(flag : String) = this.flags.add(flag)
    fun addOption(option : String) = this.options.add(option)
    fun addPositional(positional : String) = this.positional.add(positional)

    fun hasAtLeastPositional(many : Int) : Boolean  = positional.size >= many
    fun hasFlag(flag : String)    : Boolean = flag in flags
    fun hasOption(option: String) : Boolean = option in options
    fun hasNPositional(n: Int)    : Boolean = positional.size == n
}
