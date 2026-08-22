package cnc.lexer.rules

import cnc.lexer.CharStream
import cnc.token.Token
import cnc.token.TokenType

class PatternRule(
    private val startPredicate: (Char) -> Boolean,
    private val continuePredicate: (Char) -> Boolean,
    private val tokenType: TokenType
) : LexerRule {

    constructor(
        predicate: (Char) -> Boolean,
        tokenType: TokenType
    ) : this(predicate, predicate, tokenType)

    override fun tryMatch(stream: CharStream): LexResult? {
        val first = stream.peek() ?: return null
        if (!startPredicate(first)) return null

        val startPos = stream.position
        val builder = StringBuilder()
        builder.append(stream.advance()!!)

        while (stream.peek()?.let(continuePredicate) == true) {
            builder.append(stream.advance()!!)
        }

        return LexResult.Matched(Token(tokenType, startPos, builder.toString()))
    }
}
