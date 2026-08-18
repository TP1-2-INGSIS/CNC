package cnc.definition

import cnc.token.TokenType
import cnc.token.SymbolTokenDef
import cnc.token.RegexTokenDef

val PlusDefinition            = SymbolTokenDef(TokenType.OPERATOR, "+")
val MinusDefinition           = SymbolTokenDef(TokenType.OPERATOR, "-")
val DivisionDefinition        = SymbolTokenDef(TokenType.OPERATOR, "/")
val MultiplicationDefinition  = SymbolTokenDef(TokenType.OPERATOR, "*")
val EqualsDefinition          = SymbolTokenDef(TokenType.OPERATOR, "==")

val TerminationDefinition     = SymbolTokenDef(TokenType.SYMBOL, ";")
val TypeDefinition            = SymbolTokenDef(TokenType.SYMBOL, ":")
val AssignDefinition          = SymbolTokenDef(TokenType.SYMBOL, "=")

val VariableDefinition        = SymbolTokenDef(TokenType.KEYWORD, "let")

val StringTypeDefinition      = SymbolTokenDef(TokenType.VARIABLE_TYPE, "string")
val NumberTypeDefinition      = SymbolTokenDef(TokenType.VARIABLE_TYPE, "number")

val IdentifierDefinition       = RegexTokenDef(TokenType.IDENTIFIER, "[a-zA-Z_][a-zA-Z0-9_]*")
val NumberExpressionDefinition = RegexTokenDef(TokenType.NUMBER, "[0-9]+")
val StringExpressionDefinition = RegexTokenDef(TokenType.STRING, "\".*?\"")
