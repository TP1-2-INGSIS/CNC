package cnc.token

import cnc.common.Position
import cnc.common.Provider

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
  val alias: String,
  val symbols: List<String>;
  fun match(str: String) : Boolean;
} 

data class SymbolTokenDef (
  override val alias: String,
  override val symbols: List<String>

) : TokenDefinition {
  constructor(alias: String, symbol: String) : this(type, listOf(symbol))
  override fun match(str: String) : Boolean = str in symbols
}

class RegexTokenDef(
  override val alias: String,
  val regex: String

) : TokenDefinition {
  override val symbols: List<String> = listOf(regex)
  override fun match(str: String): Boolean = regex.toRegex().matches(str)
}

interface TokenDefinitionProvider : Provider<TokenType, List<TokenDefinition>> { 

  override fun getValue(type: TokenType) : List<TokenDefinition>?
  override fun getTypes() : Set<TokenType>
  
  fun type(str: String): TokenType
  fun getExpressionTokens(): List<TokenDefinition>
}
