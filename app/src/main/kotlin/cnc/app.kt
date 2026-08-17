package cnc

import cnc.config.TokenDef

import cnc.lexer.Lexer
import cnc.lexer.Parser

import cnc.common.StrContent
import cnc.common.ContentManager


data class Config (
  val lexer: Lexer = Lexer(TokenDef)
  val parser = Parser(),
  // val interpreter = Interpreter(config...)
)

data class Compiler (
  val config : Config
){
  fun compile(content: ContentManager) {
    content
    .getLines()
    .withIndex()
    .flatMap { (row, line) -> config.lexer.getTokens(line, row) }
    // .flatMap { parser methods }
    // .flatMap { interpreter }
    // podriamos devolver un Program
    // el cual contenga un metodo que
    // se .run()
    .forEach { println(it) }
  }
}


fun main() {
  val compiler = Compiler(Config())
  compiler.compile(StrContent("let var: number = 10;"))
}
