package cnc.lexer.rules

import cnc.lexer.CharStream

class WhitespaceRule(
    private val isWhitespace: (Char) -> Boolean = Char::isWhitespace
) : LexerRule {

    override fun tryMatch(stream: CharStream): LexResult? {
        val first = stream.peek() ?: return null
        if (!isWhitespace(first)) return null

        while (stream.peek()?.let(isWhitespace) == true) {
            stream.advance()
        }

        return LexResult.Skipped
    }
}
