package cnc.lexer.rules

import cnc.common.Cursor
import cnc.lexer.TrieNode
import cnc.lexer.buildTrie
import cnc.lexer.matchExact
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

    override fun tryMatch(cursor: Cursor<Char>): RuleResult? {
        val first = cursor.peek() ?: return null
        if (!isStart(first)) return null

        val builder = StringBuilder()
        builder.append(cursor.advance()!!)

        while (cursor.peek()?.let(isContinue) == true) {
            builder.append(cursor.advance()!!)
        }

        val word = builder.toString()
        val type = keywordsTrie.matchExact(word) ?: defaultType
        return RuleResult.Matched(type, word)
    }
}
