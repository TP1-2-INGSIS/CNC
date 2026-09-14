package cnc.lexer.rules

import cnc.common.Cursor
import cnc.token.TokenType

class DelimitedRule(
    private val delimiter: Char,
    private val tokenType: TokenType,
    private val escapeChar: Char? = '\\'
) : LexerRule {

    override fun tryMatch(cursor: Cursor<Char>): RuleResult? {
        val first = cursor.peek() ?: return null
        if (first != delimiter) return null

        val builder = StringBuilder()
        builder.append(cursor.advance()!!) // Consume opening delimiter

        while (cursor.hasMore()) {
            val char = cursor.advance() ?: break
            builder.append(char)

            if (escapeChar != null && char == escapeChar && cursor.hasMore()) {
                builder.append(cursor.advance()!!)
                continue
            }

            if (char == delimiter) {
                return RuleResult.Matched(tokenType, builder.toString())
            }
        }

        // Reaching EOF without closing delimiter marks token as INVALID
        return RuleResult.Matched(TokenType.INVALID, builder.toString())
    }
}
