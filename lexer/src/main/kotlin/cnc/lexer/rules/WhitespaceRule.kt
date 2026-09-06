package cnc.lexer.rules

import cnc.common.Cursor

class WhitespaceRule(
    private val isWhitespace: (Char) -> Boolean = Char::isWhitespace
) : LexerRule {

    override fun tryMatch(cursor: Cursor<Char>): RuleResult? {
        val first = cursor.peek() ?: return null
        if (!isWhitespace(first)) return null

        while (cursor.peek()?.let(isWhitespace) == true) {
            cursor.advance()
        }

        return RuleResult.Skipped
    }
}
