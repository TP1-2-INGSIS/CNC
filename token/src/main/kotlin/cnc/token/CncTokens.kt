package cnc.token

object CncKeywords {
    val LET = SymbolTokenDef("let", "let")
    val CONST = SymbolTokenDef("const", "const")
    val IF = SymbolTokenDef("if", "if")
    val ELSE = SymbolTokenDef("else", "else")
    val TRUE = SymbolTokenDef("true", "true")
    val FALSE = SymbolTokenDef("false", "false")

    val TYPE_STRING = SymbolTokenDef("string_type", "string")
    val TYPE_NUMBER = SymbolTokenDef("number_type", "number")
    val TYPE_BOOLEAN = SymbolTokenDef("boolean_type", "boolean")

    val types: List<TokenDefinition> = listOf(TYPE_STRING, TYPE_NUMBER, TYPE_BOOLEAN)

    val v1_0 = mapOf(
        "let" to TokenType.KEYWORD,
        "string" to TokenType.VARIABLE_TYPE,
        "number" to TokenType.VARIABLE_TYPE
    )

    val v1_1 = mapOf(
        "let" to TokenType.KEYWORD,
        "const" to TokenType.KEYWORD,
        "if" to TokenType.KEYWORD,
        "else" to TokenType.KEYWORD,
        "true" to TokenType.KEYWORD,
        "false" to TokenType.KEYWORD,
        "string" to TokenType.VARIABLE_TYPE,
        "number" to TokenType.VARIABLE_TYPE,
        "boolean" to TokenType.VARIABLE_TYPE
    )

    val all = v1_1
}

object CncSymbols {
    val PLUS = SymbolTokenDef("plus", "+")
    val MINUS = SymbolTokenDef("minus", "-")
    val DIVISION = SymbolTokenDef("division", "/")
    val MULTIPLICATION = SymbolTokenDef("multiplication", "*")
    val EQUALS = SymbolTokenDef("equals", "==")
    val EXPONENT = SymbolTokenDef("exponent", "**")
    val SEMICOLON = SymbolTokenDef("semicolon", ";")
    val COLON = SymbolTokenDef("colon", ":")
    val ASSIGN = SymbolTokenDef("assign", "=")
    val OPEN_PAREN = SymbolTokenDef("open_paren", "(")
    val CLOSE_PAREN = SymbolTokenDef("close_paren", ")")
    val OPEN_BRACE = SymbolTokenDef("open_brace", "{")
    val CLOSE_BRACE = SymbolTokenDef("close_brace", "}")
    val COMMA = SymbolTokenDef("comma", ",")

    val all = mapOf(
        "+" to TokenType.OPERATOR,
        "-" to TokenType.OPERATOR,
        "/" to TokenType.OPERATOR,
        "*" to TokenType.OPERATOR,
        "==" to TokenType.OPERATOR,
        "**" to TokenType.OPERATOR,
        ";" to TokenType.SYMBOL,
        ":" to TokenType.SYMBOL,
        "=" to TokenType.SYMBOL,
        "(" to TokenType.SYMBOL,
        ")" to TokenType.SYMBOL,
        "{" to TokenType.SYMBOL,
        "}" to TokenType.SYMBOL,
        "," to TokenType.SYMBOL
    )
}

object CncPatterns {
    val IDENTIFIER = RegexTokenDef("identifier", "[a-zA-Z_][a-zA-Z0-9_]*")
    val NUMBER = RegexTokenDef("number_exp", "[0-9]+")
    val STRING = RegexTokenDef("string_exp", "\".*?\"")
}
