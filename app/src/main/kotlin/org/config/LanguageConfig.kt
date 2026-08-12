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

val PlusDefinition            = SymbolTokenDef(TokenType.OPERATOR, "+")
val MinusDefinition           = SymbolTokenDef(TokenType.OPERATOR, "-")
val DivisionDefinition        = SymbolTokenDef(TokenType.OPERATOR, "/")
val MultiplicationDefinition  = SymbolTokenDef(TokenType.OPERATOR, "*")
val EqualsDefinition          = SymbolTokenDef(TokenType.OPERATOR, "==")

val TerminationDefinition   = SymbolTokenDef(TokenType.SYMBOL, ";")
val TypeDefinition          = SymbolTokenDef(TokenType.SYMBOL, ":")
val AssignDefinition        = SymbolTokenDef(TokenType.SYMBOL, "=")

val VariableDefinition      = SymbolTokenDef(TokenType.KEYWORD, "let")

val StringTypeDefinition        = SymbolTokenDef(TokenType.VARIABLE_TYPE, "string")
val NumberTypeDefinition        = SymbolTokenDef(TokenType.VARIABLE_TYPE, "number")

val IdentifierDefinition       = RegexTokenDef(TokenType.IDENTIFIER, "[a-zA-Z_][a-zA-Z0-9_]*")
val NumberExpressionDefinition           = RegexTokenDef(TokenType.NUMBER, "[0-9]+")
val StringExpressionDefinition = RegexTokenDef(TokenType.STRING, "\".*?\"")
