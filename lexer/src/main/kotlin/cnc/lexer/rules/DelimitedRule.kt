package cnc.lexer.rules

import cnc.lexer.CharStream
import cnc.token.Token
import cnc.token.TokenType

class DelimitedRule(
    private val delimiter: Char,
    private val tokenType: TokenType,
    private val escapeChar: Char? = '\\'
) : LexerRule {

    override fun tryMatch(stream: CharStream): LexResult? {
        val first = stream.peek() ?: return null
        if (first != delimiter) return null

        val startPos = stream.position
        val builder = StringBuilder()
        builder.append(stream.advance()!!) // Consumir delimitador de apertura

        while (stream.hasMore()) {
            val char = stream.advance() ?: break
            builder.append(char)

            if (escapeChar != null && char == escapeChar && stream.hasMore()) {
                builder.append(stream.advance()!!)
                continue
            }

            if (char == delimiter) {
                return LexResult.Matched(Token(tokenType, startPos, builder.toString()))
            }
        }

        // Si llega a EOF sin cerrar el delimitador, se marca como INVALID
        return LexResult.Matched(Token(TokenType.INVALID, startPos, builder.toString()))
    }
}
