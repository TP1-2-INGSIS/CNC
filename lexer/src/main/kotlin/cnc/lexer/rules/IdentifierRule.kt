package cnc.lexer.rules

import cnc.lexer.CharStream
import cnc.lexer.TrieNode
import cnc.lexer.buildTrie
import cnc.lexer.matchExact
import cnc.token.Token
import cnc.token.TokenType

class IdentifierRule(
    private val isStart: (Char) -> Boolean,
    private val isContinue: (Char) -> Boolean,
    private val keywordsTrie: TrieNode<TokenType>,
    private val defaultType: TokenType = TokenType.IDENTIFIER
) : LexerRule {

    constructor(
        isStart: (Char) -> Boolean,
        isContinue: (Char) -> Boolean,
        keywords: Map<String, TokenType> = emptyMap(),
        defaultType: TokenType = TokenType.IDENTIFIER
    ) : this(isStart, isContinue, buildTrie(keywords), defaultType)

    override fun tryMatch(stream: CharStream): LexResult? {
        val first = stream.peek() ?: return null
        if (!isStart(first)) return null

        val startPos = stream.position
        val builder = StringBuilder()
        builder.append(stream.advance()!!)

        while (stream.peek()?.let(isContinue) == true) {
            builder.append(stream.advance()!!)
        }

        val word = builder.toString()
        val type = keywordsTrie.matchExact(word) ?: defaultType
        return LexResult.Matched(Token(type, startPos, word))
    }
}
