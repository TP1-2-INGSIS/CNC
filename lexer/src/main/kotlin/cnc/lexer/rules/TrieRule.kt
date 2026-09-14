package cnc.lexer.rules

import cnc.common.Cursor
import cnc.common.consume
import cnc.lexer.TrieNode
import cnc.lexer.buildTrie
import cnc.lexer.matchLongest
import cnc.token.TokenType

class TrieRule(
    private val root: TrieNode<TokenType>
) : LexerRule {

    constructor(symbols: Map<String, TokenType>) : this(buildTrie(symbols))

    override fun tryMatch(cursor: Cursor<Char>): RuleResult? {
        val (type, length) = root.matchLongest(cursor) ?: return null
        if (length == 0) return null

        val text = cursor.consume(length)
        return RuleResult.Matched(type, text)
    }
}
