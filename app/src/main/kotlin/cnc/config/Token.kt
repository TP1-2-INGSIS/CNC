package cnc.config

import cnc.token.Token
import cnc.token.TokenType
import cnc.token.RegexTokenDef
import cnc.token.SymbolTokenDef
import cnc.token.TokenDefinition

object CncKeywords {
  val LET = SymbolTokenDef("let", "let")
  val TYPE_STRING = SymbolTokenDef("string_type", "string")
  val TYPE_NUMBER = SymbolTokenDef("number_type", "number")

  val types: List<TokenDefinition> = listOf(TYPE_STRING, TYPE_NUMBER)

  val all = mapOf(
    "let" to TokenType.KEYWORD,
    "string" to TokenType.VARIABLE_TYPE,
    "number" to TokenType.VARIABLE_TYPE
  )
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

  val all = mapOf(
    "+" to TokenType.OPERATOR,
    "-" to TokenType.OPERATOR,
    "/" to TokenType.OPERATOR,
    "*" to TokenType.OPERATOR,
    "==" to TokenType.OPERATOR,
    "**" to TokenType.OPERATOR,
    ";" to TokenType.SYMBOL,
    ":" to TokenType.SYMBOL,
    "=" to TokenType.SYMBOL
  )
}

object CncPatterns {
  val IDENTIFIER = RegexTokenDef("identifier", "[a-zA-Z_][a-zA-Z0-9_]*")
  val NUMBER = RegexTokenDef("number_exp", "[0-9]+")
  val STRING = RegexTokenDef("string_exp", "\".*?\"")
}
