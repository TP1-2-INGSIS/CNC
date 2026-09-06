package cnc.lexer

import cnc.common.Cursor
import cnc.lexer.rules.LexerRule
import cnc.lexer.rules.RuleResult
import cnc.token.RawToken
import cnc.token.TokenType

class Lexer(
    private val rules: List<LexerRule>
) {

    constructor(vararg rules: LexerRule) : this(rules.toList())

    /**
     * Tokenizes characters from the given [cursor] into a lazy sequence of [RawToken]s.
     */
    fun tokenize(cursor: Cursor<Char>): Sequence<RawToken> = sequence {
        while (cursor.hasMore()) {
            val startOffset = cursor.currentOffset
            when (val result = rules.firstNotNullOfOrNull { it.tryMatch(cursor) }) {
                is RuleResult.Matched -> yield(RawToken(result.type, result.text, startOffset))
                is RuleResult.Skipped -> Unit
                null -> {
                    val ch = cursor.advance().toString()
                    yield(RawToken(TokenType.INVALID, ch, startOffset))
                }
            }
        }
    }
}
