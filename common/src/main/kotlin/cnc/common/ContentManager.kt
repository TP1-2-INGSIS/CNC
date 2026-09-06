package cnc.common

import java.io.File
import java.io.Reader
import java.io.StringReader

/**
 * Default buffer size for stream reading (8 KB), aligned with filesystem block sizes and CPU L1 cache.
 */
const val DEFAULT_STREAM_BUFFER_SIZE = 8192

interface ContentManager {
    fun getReader(): Reader
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

class StrContent(val content: String) : ContentManager {
    override fun getReader(): Reader = StringReader(content)
}

/**
 * Encapsulates the streaming [cursor] and incremental [lineIndex] for a source text.
 */
data class SourceStream(
    val cursor: Cursor<Char>,
    val lineIndex: LineIndex
)

/**
 * Lazily streams characters from this [ContentManager] into a [Cursor] and builds the [LineIndex]
 * incrementally on-the-fly in a single pass with O(1) text memory.
 */
fun ContentManager.openStream(bufferSize: Int = DEFAULT_STREAM_BUFFER_SIZE): SourceStream {
    val lineIndex = LineIndex()
    val charSequence = sequence {
        getReader().use { reader ->
            val buffer = CharArray(bufferSize)
            var offset = 0
            var read = reader.read(buffer)
            while (read != -1) {
                for (i in 0 until read) {
                    val ch = buffer[i]
                    if (ch == '\n') {
                        lineIndex.recordLineStart(offset + 1)
                    }
                    yield(ch)
                    offset++
                }
                read = reader.read(buffer)
            }
        }
    }
    return SourceStream(charSequence.asCursor(), lineIndex)
}
