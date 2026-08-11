package org.config

import org.utils.Position

enum class TokenType {
  OPERATOR,
  SYMBOL,
  IDENTIFIER,
  STRING,
  NUMBER,
  KEYWORD,
  VARIABLE_TYPE,
  INVALID
};

data class Token(
  val type: TokenType,
  val pos: Position, // no guardo la position final, porque tenemos el size del texto
  val text: String
)

interface TokenDefinition {
  val type: TokenType;
  val symbols: List<String>;
  fun match(value: String) : Boolean;
} 

data class SymbolTokenDef (
  override val type: TokenType,
  override val symbols: List<String>
) : TokenDefinition {

  constructor(type: TokenType, symbol: String) : this(type, listOf(symbol))
  override fun match(str: String) : Boolean = str in symbols
}

class RegexTokenDef(
  override val type: TokenType,
  val regex: String
) : TokenDefinition {
  override val symbols: List<String> = listOf(regex)
  override fun match(other: String): Boolean = regex.toRegex().matches(other)
}

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
