package cnc.common

import java.io.BufferedReader
import java.io.File

interface ContentManager {
  fun getLines(): Sequence<String>
}

class FileContent(
  val path: String,
) : ContentManager {
  val buffer: BufferedReader

  init {
    val file = File(path)
    require(file.exists()) { "The path provided does not exists." }
    require(file.isFile()) { "The path provided is not a file." }
    require(file.canRead()) { "The file provided is not available to read." }
    buffer = File(path).bufferedReader()
  }

  override fun getLines(): Sequence<String> = buffer.lineSequence()
}

class StrContent(
  var content: String,
) : ContentManager {
  fun getNextLine(): String {
    val result = content
    content = ""
    return result
  }

  override fun getLines(): Sequence<String> = listOf(getNextLine()).asSequence()
}
