package cnc.lexer.rules

import cnc.lexer.CharStream
import cnc.lexer.TrieNode
import cnc.lexer.buildTrie
import cnc.lexer.matchLongest
import cnc.token.Token
import cnc.token.TokenType

class TrieRule(
    private val root: TrieNode<TokenType>
) : LexerRule {

    constructor(symbols: Map<String, TokenType>) : this(buildTrie(symbols))

    override fun tryMatch(stream: CharStream): LexResult? {
        val startPos = stream.position
        val (type, length) = root.matchLongest(stream) ?: return null
        if (length == 0) return null

        val text = stream.consume(length)
        return LexResult.Matched(Token(type, startPos, text))
    }
}
