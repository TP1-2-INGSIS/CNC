package cnc

import cnc.Lexer
import cnc.StrContent
import cnc.Parser

fun main() {
  val tokens = Lexer.getTokens(StrContent("let name: string = \"John\";"))
  val ast = Parser.getAST(tokens)
  ast.toList().forEach { print(it) }
}
