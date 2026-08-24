package cnc.lexer

import cnc.common.Position
import cnc.common.advance
import java.io.Reader

class CharStream(private val reader: Reader) {
    private var _position: Position = Position(0, 0)
    private val buffer = mutableListOf<Char>()

    val position: Position
        get() = _position

    fun hasMore(): Boolean = peek() != null

    fun peek(offset: Int = 0): Char? {
        while (buffer.size <= offset) {
            val charCode = reader.read().takeIf { it != -1 } ?: return null
            buffer.add(charCode.toChar())
        }
        return buffer[offset]
    }

    fun advance(): Char? {
        val char = peek() ?: return null
        buffer.removeFirst()
        _position = _position.advance(char)
        return char
    }

    fun consume(count: Int): String = buildString(count) {
        repeat(count) {
            append(advance() ?: return@buildString)
        }
    }
}
