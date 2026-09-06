package cnc.lexer.rules

import cnc.common.Cursor
import cnc.token.TokenType

sealed interface RuleResult {
    data class Matched(val type: TokenType, val text: String) : RuleResult
    data object Skipped : RuleResult
}

fun interface LexerRule {
    fun tryMatch(cursor: Cursor<Char>): RuleResult?
}
