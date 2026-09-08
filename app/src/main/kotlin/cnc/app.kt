package cnc

import cnc.common.ContentManager
import cnc.common.Failure
import cnc.common.FileContent
import cnc.common.Success
import cnc.common.openStream
import cnc.config.*
import cnc.lexer.Lexer
import cnc.parser.Parser
import cnc.semantic.SemanticAnalyzer

data class Config(
  val lexer: Lexer = printScriptLexer,
  val parser: Parser = printScriptParser,
  val semantic: SemanticAnalyzer = SemanticAnalyzer(semanticContext)
)

data class Compiler(
  val config: Config
) {
  fun compile(content: ContentManager) {
    val cursor = content.openStream()
    val tokens = config.lexer.tokenize(cursor)
    val statements = config.parser.getASTs(tokens)
    config.semantic.analyze(statements).forEach { result ->
      when (result) {
        is Success -> println("OK: ${result.data}")
        is Failure -> println("ERROR: ${result.msg}")
      }
    }
  }

  fun compile(path: String) = compile(FileContent(path))
}

fun main() {
  CLISystem.run()
}
