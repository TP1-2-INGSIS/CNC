package cnc.common

import java.io.File
import java.io.Reader
import java.io.StringReader

interface ContentManager {
  fun getReader(): Reader
}

class FileContent(val path: String) : ContentManager {
  init {
    val file = File(path)
    require(file.exists()) { "The path provided does not exist." }
    require(file.isFile()) { "The path provided is not a file." }
    require(file.canRead()) { "The file provided is not available to read." }
  }

  override fun getReader(): Reader = File(path).reader()
}

class StrContent(val content: String) : ContentManager {
  override fun getReader(): Reader = StringReader(content)
}

