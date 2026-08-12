package org

import org.lexer.Lexer
import org.lexer.StrContent
import org.parser.Parser

fun main() {
  val tokens = Lexer.getTokens(StrContent("let name: string = \"John\";"))
  val ast = Parser.getAST(tokens)
  ast.toList().forEach { print(it) }
}
