package org.config

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

val PlusDefinition            = TokenDef(TokenType.OPERATOR, "+")
val MinusDefinition           = TokenDef(TokenType.OPERATOR, "-")
val DivisionDefinition        = TokenDef(TokenType.OPERATOR, "/")
val MultiplicationDefinition  = TokenDef(TokenType.OPERATOR, "*")
val EqualsDefinition          = TokenDef(TokenType.OPERATOR, "==")

val TerminationDefinition   = TokenDef(TokenType.SYMBOL, ";")
val TypeDefinition          = TokenDef(TokenType.SYMBOL, ":")
val AssignDefinition        = TokenDef(TokenType.SYMBOL, "=")

val VariableDefinition      = TokenDef(TokenType.KEYWORD, "let")

val StringTypeDefinition        = TokenDef(TokenType.VARIABLE_TYPE, "string")
val NumberTypeDefinition        = TokenDef(TokenType.VARIABLE_TYPE, "number")

val IdentifierDefinition       = RegexTokenDef(TokenType.IDENTIFIER, "[a-zA-Z_][a-zA-Z0-9_]*")
val NumberExpressionDefinition           = RegexTokenDef(TokenType.NUMBER, "[0-9]+")
val StringExpressionDefinition = RegexTokenDef(TokenType.STRING, "\".*?\"")
