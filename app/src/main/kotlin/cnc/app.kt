package cnc

import cnc.config.*
import cnc.lexer.Lexer
import cnc.common.StrContent
import cnc.common.ContentManager
import cnc.parser.Parser
import cnc.semantic.SemanticAnalyzer
import cnc.semantic.SemanticVisitor

data class Config (
  val lexer: Lexer = Lexer(PrintScriptTokenDefProvider),
  val parser: Parser = Parser(grammars, terminators),
  val semantic: SemanticAnalyzer = SemanticAnalyzer(SemanticVisitor(binaryTypeRules, symbolTable))
)

data class Compiler (
  val config : Config
){
  fun compile(content: ContentManager) {
    val tokens = content
      .getLines()
      .withIndex()
      .flatMap { (row, line) -> config.lexer.getTokens(line, row) }

    val statements = config.parser.getASTs(tokens)
    config.semantic.analyze(statements)
  }
}

fun main() {
  val compiler = Compiler(Config())
  compiler.compile(StrContent(
    "let x: number = 10;\n" +
    "let y: string = x;\n" +
    "let x: number = 5;\n" +
    "println(x);"
  ))
}
