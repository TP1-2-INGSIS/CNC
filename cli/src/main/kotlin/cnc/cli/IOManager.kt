package cnc.cli

interface IOManager {
    fun read() : String
    fun write(toWrite: String)
}

class StdIO : IOManager {
    override fun read() : String = readln()

    override fun write(message: String) { print(message) }
}
