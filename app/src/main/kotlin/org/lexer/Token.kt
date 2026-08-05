package org.lexer

import org.utils.Position

enum class TokenType {
  END,
  INVALID,
  HASH,
  IDENTIFIER,
  PUNTUATION,
  OPERATOR,
  LITERAL
};

data class Token(
  val type: TokenType,
  val pos: Position, // no guardo la position final, porque tenemos el size del texto
  val text: String
);
