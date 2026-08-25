package cnc.lexer.rules

import cnc.lexer.CharStream
import cnc.token.Token

sealed interface LexResult {
    data class Matched(val token: Token) : LexResult
    data object Skipped : LexResult
}

fun interface LexerRule {
    fun tryMatch(stream: CharStream): LexResult?
}
