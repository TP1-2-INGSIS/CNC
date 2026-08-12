package org.config

import cnc.token.Token

// TODO: separar esto en varios files, se me ocurre desde el root app\src\..\config\
// hacer definitions\, providers\, expressions\ y grammars\
// con el fin de usarlo como cnc.config.expressions.{especifico} y asi.
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

// unica fuente de verdad
object TokenDefinitionProvider {
  val definitions = mapOf(
    TokenType.OPERATOR to listOf(
      PlusDefinition,
      MinusDefinition,
      DivisionDefinition,
      MultiplicationDefinition,
      EqualsDefinition
    ),
    TokenType.SYMBOL to listOf(
      TerminationDefinition,
      TypeDefinition,
      AssignDefinition
    ),
    TokenType.VARIABLE_TYPE to listOf(
      StringTypeDefinition,
      NumberTypeDefinition
    ),
    TokenType.KEYWORD to listOf(VariableDefinition),
    TokenType.IDENTIFIER  to listOf(IdentifierDefinition),
    TokenType.NUMBER      to listOf(NumberExpressionDefinition),
    TokenType.STRING      to listOf(StringExpressionDefinition)
  )

  fun getDefinitions(type: TokenType) : List<TokenDefinition>? = definitions[type]

  fun getTypes() : Set<TokenType> = definitions.keys
}

val expressionBuilder = ExpressionBuilder(mapOf(
  NumberExpressionDefinition to { token: Token -> NumberLiteral(token.text.toDouble()) },
  StringExpressionDefinition to { token: Token -> StringLiteral(token.text.removeSurrounding("\"")) },
  IdentifierDefinition to { token -> Identifier(token.text) }
))
