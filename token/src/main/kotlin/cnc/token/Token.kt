package cnc.token

import cnc.common.Position

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

