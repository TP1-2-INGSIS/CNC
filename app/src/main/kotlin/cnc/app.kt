package cnc

import cnc.config.*
import cnc.lexer.Lexer
import cnc.common.StrContent
import cnc.common.ContentManager
import cnc.common.Success
import cnc.common.Failure
import cnc.parser.Parser
import cnc.semantic.SemanticAnalyzer
import cnc.semantic.SemanticVisitor

data class Config (
  val lexer: Lexer = printScriptLexer,
  val parser: Parser = Parser(grammars, terminators),
  val semantic: SemanticAnalyzer = SemanticAnalyzer(SemanticVisitor(binaryTypeRules, symbolTable))
)

data class Compiler (
  val config : Config
){
  fun compile(content: ContentManager) {
    val tokens = config.lexer.tokenize(content)
    val statements = config.parser.getASTs(tokens)
    config.semantic.analyze(statements).forEach { result ->
      when (result) {
        is Success -> println("OK: ${result.data}")
        is Failure -> println("ERROR: ${result.msg}")
      }
    }
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
