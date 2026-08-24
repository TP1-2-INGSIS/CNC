package cnc.cli.args

object ArgsManager {
  fun tokenize(commandLine: String): List<String> {
    val tokens = mutableListOf<String>()
    val current = StringBuilder()
    var insideQuotes = false
    var quoteChar = ' '

    for (c in commandLine) {
      if ((c == '"' || c == '\'') && !insideQuotes) {
        insideQuotes = true
        quoteChar = c
        continue
      }

      if (c == quoteChar && insideQuotes) {
        insideQuotes = false
        continue
      }

      if (c.isWhitespace() && !insideQuotes) {
        if (current.isNotEmpty()) {
          tokens.add(current.toString())
          current.clear()
        }
        continue
      }

      current.append(c)
    }

    if (current.isNotEmpty()) {
      tokens.add(current.toString())
    }

    return tokens
  }

  fun getArgsContainer(args: List<String>): ArgsContainer {
    val container = ArgsContainer()

    for (arg in args) {
      if (arg.startsWith("-") && arg.contains("=")) {
        val equalIdx = arg.indexOf('=')
        val key = arg.substring(0, equalIdx)
        val rawValue = arg.substring(equalIdx + 1)
        val cleanValue = rawValue.removeSurrounding("\"").removeSurrounding("'")
        container.addOption(key, cleanValue)
        continue
      }

      if (arg.startsWith("-")) {
        container.addFlag(arg)
        continue
      }

      val cleanArg = arg.removeSurrounding("\"").removeSurrounding("'")
      container.addPositional(cleanArg)
    }

    return container
  }
}
