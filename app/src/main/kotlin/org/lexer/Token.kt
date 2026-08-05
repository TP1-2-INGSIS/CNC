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
  val pos: Position,
  val text: String
);
