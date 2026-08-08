package org.config

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
  
  // como recorro la lista, el orden importa.
  // TODO: Necesitamos separar mas las responsabilidades
  // el TokenType.OPERATOR deberia estar definido en otro lado
  // donde sea mas facil el mantenerlo a futuro. Ahora si el cliente
  // quiere agregar un nuevo operador tiene que venir y cambiar esto.
  // Malissimo.
  //
  // Deberia ser algo del estilo TokenRule(TokenType.OPERATOR) { OperatorDef.check(it) }
  // entonces ahi podemos abstraer todas las definiciones a otro archivo separado y
  // manejarlo de mejor manera.
  //
  // Actualmente el regex del splitter tiene una "def" de que es un IDENTIFIER
  // que podria ser diferente a la que usamos aca.
  val rules = listOf(
    TokenRule(TokenType.LET)        { it == "let" },
    TokenRule(TokenType.ASSIGN)     { it == "=" },
    TokenRule(TokenType.SEMICOLON)  { it == ";" },
    TokenRule(TokenType.COLON)      { it == ":" },
    TokenRule(TokenType.OPERATOR)   { setOf("+","-", "/", "*").contains(it) },
    TokenRule(TokenType.TYPE)       { setOf("number", "string").contains(it) },
    TokenRule(TokenType.IDENTIFIER) { it.matches(Regex("[a-zA-Z_][a-zA-Z0-9_]*")) },
    TokenRule(TokenType.LITERAL)    { it.matches(Regex("\\d+")) },
    TokenRule(TokenType.STRING)     { it.matches(Regex("\".*\"")) },
  )

  fun type(token: String): TokenType = rules.find { it.eval(token) }?.type ?: TokenType.INVALID 
}
