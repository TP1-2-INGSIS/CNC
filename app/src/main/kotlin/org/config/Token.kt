package org.config

import org.utils.Position

enum class TokenType {
  OPERATOR,
  SYMBOL,
  INDENTIFIER,
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
);
