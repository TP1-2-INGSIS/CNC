package cnc

import cnc.config.*
import cnc.lexer.Lexer
import cnc.common.FileContent
import cnc.common.ContentManager
import cnc.common.Success
import cnc.common.Failure
import cnc.parser.Parser
import cnc.semantic.SemanticAnalyzer

data class Config(
  val lexer: Lexer = printScriptLexer,
  val parser: Parser = Parser(grammars, terminators),
  val semantic: SemanticAnalyzer = SemanticAnalyzer(semanticContext)
)

data class Compiler(
  val config: Config
) {
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
  CLISystem.run()
}
