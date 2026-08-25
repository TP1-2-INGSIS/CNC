package cnc.lexer

import cnc.common.ContentManager
import cnc.common.StrContent
import cnc.lexer.rules.LexResult
import cnc.lexer.rules.LexerRule
import cnc.token.Token
import cnc.token.TokenType

class Lexer(
    private val rules: List<LexerRule>
) {

    constructor(vararg rules: LexerRule) : this(rules.toList())

    fun tokenize(content: ContentManager): Sequence<Token> = sequence {
        val stream = CharStream(content.getReader())
        while (stream.hasMore()) {
            when (val step = nextStep(stream)) {
                is LexResult.Matched -> yield(step.token)
                is LexResult.Skipped -> Unit
            }
        }
    }

    fun getTokens(content: ContentManager): Sequence<Token> = tokenize(content)

    fun getTokens(line: String, row: Int = 0): Sequence<Token> = tokenize(StrContent(line))

    private fun nextStep(stream: CharStream): LexResult =
        rules.firstNotNullOfOrNull { it.tryMatch(stream) }
            ?: LexResult.Matched(Token(TokenType.INVALID, stream.position, stream.advance().toString()))
}
