package cnc.lexer

import cnc.common.Position
import java.io.Reader

class CharStream(private val reader: Reader) {
    private var _position: Position = Position(0, 0)
    private val buffer = mutableListOf<Char>()

    val position: Position
        get() = _position

    fun hasMore(): Boolean = peek() != null

    fun peek(offset: Int = 0): Char? {
        while (buffer.size <= offset) {
            val charCode = reader.read()
            if (charCode == -1) return null
            buffer.add(charCode.toChar())
        }
        return buffer[offset]
    }

    fun advance(): Char? {
        val char = if (buffer.isNotEmpty()) {
            buffer.removeAt(0)
        } else {
            val charCode = reader.read()
            if (charCode == -1) return null
            charCode.toChar()
        }

        _position = _position.advance(char)
        return char
    }

    fun consume(count: Int): String {
        val builder = StringBuilder(count)
        repeat(count) {
            val char = advance() ?: return builder.toString()
            builder.append(char)
        }
        return builder.toString()
    }
}
