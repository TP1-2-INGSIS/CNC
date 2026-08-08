package org

// TODO: Que el content este en lexer, no tiene sentido ajjaja
import org.lexer.StrContent
import org.lexer.Lexer
import org.parser.Parser

fun main() {
  val content = StrContent("let name: string = \"John\";");
  val tokens = Lexer.getTokens(content);
  Parser.parse(tokens);
}
