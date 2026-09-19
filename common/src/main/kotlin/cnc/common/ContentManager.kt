package cnc.common

import java.io.File
import java.io.InputStream
import java.io.InputStreamReader
import java.io.Reader
import java.io.StringReader

/**
 * Default buffer size for stream reading (8 KB), aligned with filesystem block sizes and CPU L1 cache.
 */
const val DEFAULT_STREAM_BUFFER_SIZE = 8192

interface ContentManager {
    fun getReader(): Reader

    companion object {
        fun of(string: String): ContentManager = StringContent(string)
        fun of(file: File): ContentManager = FileContent(file.absolutePath)
        fun of(stream: InputStream): ContentManager = InputStreamContent(stream)
    }
}

class FileContent(val path: String) : ContentManager {
    init {
        val file = File(path)
        require(file.exists()) { "The path provided does not exist: $path" }
        require(file.isFile) { "The path provided is not a file: $path" }
        require(file.canRead()) { "The file provided is not available to read: $path" }
    }

    override fun getReader(): Reader = File(path).bufferedReader()
}

class StringContent(val content: String) : ContentManager {
    override fun getReader(): Reader = StringReader(content)
}

class InputStreamContent(val stream: InputStream) : ContentManager {
    override fun getReader(): Reader = InputStreamReader(stream)
}

/**
 * Lazily streams characters from this [ContentManager] into a [CharCursor]
 * in a single pass with O(1) text memory and zero-allocation position tracking.
 */
fun ContentManager.openStream(bufferSize: Int = DEFAULT_STREAM_BUFFER_SIZE): CharCursor {
    val charSequence = sequence {
        getReader().use { reader ->
            val buffer = CharArray(bufferSize)
            var read = reader.read(buffer)
            while (read != -1) {
                for (i in 0 until read) {
                    yield(buffer[i])
                }
                read = reader.read(buffer)
            }
        }
    }
    return charSequence.asCharCursor()
}

