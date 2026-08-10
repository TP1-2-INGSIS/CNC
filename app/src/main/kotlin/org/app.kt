package org

import org.lexer.Lexer
import org.lexer.CharLexer
import org.lexer.StrContent
import org.parser.Parser

fun main() {
  val lexer: Lexer = CharLexer()
  val content = StrContent("let name: string = \"John\";")
  val tokens = lexer.getTokens(content)
  Parser.parse(tokens)
}
