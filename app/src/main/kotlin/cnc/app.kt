package cnc

import cnc.config.TokenDef

// Importamos el Lexer y las definiciones para el mismo
import cnc.lexer.Lexer
import cnc.common.StrContent
import cnc.common.ContentManager

// Como hicimos con el Lexer vamos a importar las definiciones necesarias
import cnc.parser.Parser
import cnc.definition.grammars
import cnc.definition.terminator


data class Config (
  val lexer: Lexer = Lexer(TokenDef),
  val parser: Parser = Parser(grammars, terminator),
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
    .let { config.parser.getASTs(it) }
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
