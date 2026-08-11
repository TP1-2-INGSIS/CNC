package org.config

interface TokenDefinition 

data class TokenDef (
  val type: TokenType,
  val symbols: String
) : TokenDefinition

class RegexTokenDef(
  val type: TokenType,
  val regex: String
) : TokenDefinition {
  override fun equals(other: Any?): Boolean {
    if (other !is String) return false
    return regex.containMatchIn(other)
  }
}

// "hola **2"
// stream chars
// .trim()
// "hola**2"
// .consume("+-/*%") -> Token(symbol, pos)
// "hola2"
// .startsWithLetter() -> Token(TokeType.IDENTIFIER, pos)
// -> Token(TokenType.Number, pos)
//
// AssignOperatorDefinition = TokenDef(TokenType.Symbol, "=")
// En el parser, me llega *token* = Token(TokenType.Symbol, Position(col, row), "="):
// Symbols = mapOf(AssignOperatorDefinition.symbol to method)
// token.type == Symbol ==> Symbols[token.text]() 
//
// es la expresion de una suma.
// expression = listOf(NumberDefinition, PlusOperatorDefinition, NumberDefinition)
//
// interface state
// class start(token) : state { fun consume() : Token? }
//
// TODO: TokenDef provider

val PlusOperatorDefinition            = TokenDef(TokenType.OPERATOR, "+")
val MinusOperatorDefinition           = TokenDef(TokenType.OPERATOR, "-")
val DivisionOperatorDefinition        = TokenDef(TokenType.OPERATOR, "/")
val MultiplicationOperatorDefinition  = TokenDef(TokenType.OPERATOR, "*")
val EqualsOperatorDefinition          = TokenDef(TokenType.OPERATOR, "==")

val TerminationDefinition   = TokenDef(TokenType.SYMBOL, ";")
val TypeDefinition          = TokenDef(TokenType.SYMBOL, ":")
val AssignDefinition        = TokenDef(TokenType.SYMBOL, "=")

val VariableDefinition      = TokenDef(TokenType.KEYWORD, "let")

val StringDefinition        = TokenDef(TokenType.VARIABLE_TYPE, "string")
val NumberDefinition        = TokenDef(TokenType.VARIABLE_TYPE, "number")

val IdentifierDefinition       = RegexTokenDef(TokenType.IDENTIFIER, "[a-zA-Z_][a-zA-Z0-9_]*")
val NumberDefinition           = RegexTokenDef(TokenType.NUMBER, "[0-9]+")
val StringExpressionDefinition = RegexTokenDef(TokenType.STRING, "\".*\"")
