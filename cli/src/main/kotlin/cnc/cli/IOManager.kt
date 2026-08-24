package cnc.cli

interface IOManager {
  fun read(): String

  fun write(toWrite: String)

  fun writeLine(toWrite: String = "") {
    write(toWrite + "\n")
  }
}

class StdIO : IOManager {
  override fun read(): String = readlnOrNull() ?: "exit"

  override fun write(toWrite: String) {
    print(toWrite)
  }
}

class TestIO(
  private val inputs: MutableList<String> = mutableListOf(),
) : IOManager {
  private val outputs = StringBuilder()

  fun addInput(input: String) {
    inputs.add(input)
  }

  fun getOutput(): String = outputs.toString()

  override fun read(): String {
    if (inputs.isEmpty()) {
      return "exit"
    }
    return inputs.removeAt(0)
  }

  override fun write(toWrite: String) {
    outputs.append(toWrite)
  }
}
