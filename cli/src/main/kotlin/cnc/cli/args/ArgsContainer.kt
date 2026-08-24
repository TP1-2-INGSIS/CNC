package cnc.cli.args

enum class ParamType {
  POSITIONAL,
  OPTIONAL,
  FLAG,
}

data class Param(
  val name: String,
  val description: String,
  val type: ParamType,
)

data class ArgsContainer(
  val flags: MutableSet<String> = mutableSetOf(),
  val options: MutableMap<String, String> = mutableMapOf(),
  val positional: MutableList<String> = mutableListOf(),
) {
  fun addFlag(flag: String) {
    flags.add(normalize(flag))
  }

  fun addOption(
    key: String,
    value: String,
  ) {
    options[normalize(key)] = value
  }

  fun addPositional(positional: String) {
    this.positional.add(positional)
  }

  fun hasFlag(flag: String): Boolean = normalize(flag) in flags

  fun hasOption(option: String): Boolean = normalize(option) in options

  fun getOption(option: String): String? = options[normalize(option)]

  fun getPositional(index: Int): String? = positional.getOrNull(index)

  fun hasAtLeastPositional(many: Int): Boolean = positional.size >= many

  fun hasNPositional(n: Int): Boolean = positional.size == n

  private fun normalize(name: String): String = name.removePrefix("--").removePrefix("-")
}
