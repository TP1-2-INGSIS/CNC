package cnc.lexer.rules

import cnc.token.TokenType

object StandardRules {

    fun whitespace(isWhitespace: (Char) -> Boolean = Char::isWhitespace): LexerRule =
        WhitespaceRule(isWhitespace)

    fun doubleQuotedString(
        tokenType: TokenType = TokenType.STRING,
        escapeChar: Char? = '\\'
    ): LexerRule = DelimitedRule(
        delimiter = '"',
        tokenType = tokenType,
        escapeChar = escapeChar
    )

    fun singleQuotedString(
        tokenType: TokenType = TokenType.STRING,
        escapeChar: Char? = '\\'
    ): LexerRule = DelimitedRule(
        delimiter = '\'',
        tokenType = tokenType,
        escapeChar = escapeChar
    )

    fun integerNumber(
        tokenType: TokenType = TokenType.NUMBER
    ): LexerRule = PatternRule(
        startPredicate = Char::isDigit,
        continuePredicate = Char::isDigit,
        tokenType = tokenType
    )

    fun standardIdentifier(
        keywords: Map<String, TokenType> = emptyMap(),
        defaultType: TokenType = TokenType.IDENTIFIER
    ): LexerRule = IdentifierRule(
        isStart = { it.isLetter() || it == '_' },
        isContinue = { it.isLetterOrDigit() || it == '_' },
        keywords = keywords,
        defaultType = defaultType
    )

    fun symbols(
        symbols: Map<String, TokenType>
    ): LexerRule = TrieRule(symbols)
}
