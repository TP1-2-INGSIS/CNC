package cnc.lexer

import cnc.common.CharCursor
import cnc.common.asCharCursor
import cnc.lexer.rules.LexerRule
import cnc.lexer.rules.RuleResult
import cnc.token.Token
import cnc.token.TokenType

class Lexer(
    private val rules: List<LexerRule>
) {

    constructor(vararg rules: LexerRule) : this(rules.toList())

    /**
     * Tokenizes characters from the given [cursor] into a lazy sequence of [Token]s with positions.
     */
    fun tokenize(cursor: CharCursor): Sequence<Token> = sequence {
        while (cursor.hasMore()) {
            val startPos = cursor.currentPosition
            when (val result = rules.firstNotNullOfOrNull { it.tryMatch(cursor) }) {
                is RuleResult.Matched -> yield(Token(result.type, startPos, result.text))
                is RuleResult.Skipped -> Unit
                null -> {
                    val ch = cursor.advance().toString()
                    yield(Token(TokenType.INVALID, startPos, ch))
                }
            }
        }
    }

    /**
     * Convenience overload to tokenize any [CharSequence] directly.
     */
    fun tokenize(source: CharSequence): Sequence<Token> = tokenize(source.asCharCursor())
}

