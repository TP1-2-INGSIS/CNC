package cnc

import cnc.config.*
import cnc.lexer.Lexer
import cnc.common.StrContent
import cnc.common.ContentManager
import cnc.parser.Parser

data class Config (
  val lexer: Lexer = printScriptLexer,
  val parser: Parser = Parser(grammars, terminators)
)

data class Compiler (
  val config : Config
){
  fun compile(content: ContentManager) {
    config.lexer.tokenize(content)
      .let { config.parser.getASTs(it) }
      .forEach { println(it) }
  }
}

fun main() {
  val compiler = Compiler(Config())
  compiler.compile(StrContent("let var: number = 10;\nvar = 4;"))
}
