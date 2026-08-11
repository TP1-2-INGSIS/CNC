package org

import org.lexer.Lexer
import org.lexer.StrContent
import org.parser.Parser

fun main() {
  val content = StrContent("let name: string = \"John\";")
  val tokens = Lexer.getTokens(content)
  tokens.toList().forEach { print(it) }
}
