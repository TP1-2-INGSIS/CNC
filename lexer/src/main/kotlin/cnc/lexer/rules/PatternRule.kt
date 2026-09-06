package cnc.lexer.rules

import cnc.common.Cursor
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

    override fun tryMatch(cursor: Cursor<Char>): RuleResult? {
        val first = cursor.peek() ?: return null
        if (!startPredicate(first)) return null

        val builder = StringBuilder()
        builder.append(cursor.advance()!!)

        while (cursor.peek()?.let(continuePredicate) == true) {
            builder.append(cursor.advance()!!)
        }

        return RuleResult.Matched(tokenType, builder.toString())
    }
}
