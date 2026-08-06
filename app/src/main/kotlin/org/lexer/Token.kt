package org.lexer

import org.utils.Position

enum class TokenType {
  LET,
  SEMICOLON,
  COLON,
  IDENTIFIER,
  OPERATOR,
  TYPE, // subset of keywords
  ASSIGN,
  LITERAL,
  STRING,
  INVALID
};

data class Token(
  val type: TokenType,
  val pos: Position, // no guardo la position final, porque tenemos el size del texto
  val text: String
);

data class TokenRule(
  val type: TokenType,
  val eval: (String) -> Boolean
)

object TokenIdentifier {

  val rules = listOf(
    TokenRule(TokenType.LET) {it == "let"},
    TokenRule(TokenType.ASSIGN) {it == "="},
    TokenRule(TokenType.SEMICOLON) {it == ";"},
    TokenRule(TokenType.COLON) {it == ":"},
    TokenRule(TokenType.OPERATOR) { setOf("+","-", "/", "*").contains(it) },
    TokenRule(TokenType.TYPE) {setOf("number", "string").contains(it)},
    TokenRule(TokenType.IDENTIFIER) {it.matches(Regex("[a-zA-Z_][a-zA-Z0-9_]*"))},
    TokenRule(TokenType.LITERAL) {it.matches(Regex("\\d+"))},
    TokenRule(TokenType.STRING) {it.matches(Regex("\".*\""))},
  )

  fun type(token: String): TokenType { 
    for (rule in rules)
      if (rule.eval(token)) return rule.type
    return TokenType.INVALID
  }
}
