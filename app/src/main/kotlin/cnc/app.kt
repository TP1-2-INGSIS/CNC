package cnc

import cnc.ast.Statement
import cnc.common.ContentManager
import cnc.common.Failure
import cnc.common.FileContent
import cnc.common.Result
import cnc.common.Success
import cnc.common.openStream
import cnc.config.*
import cnc.interpreter.Environment
import cnc.interpreter.Interpreter
import cnc.lexer.Lexer
import cnc.parser.Parser
import cnc.semantic.SemanticAnalyzer

data class Config(
  val lexer: Lexer = printScriptLexer,
  val parser: Parser = printScriptParser,
  val semantic: SemanticAnalyzer = SemanticAnalyzer(semanticContext),
  val interpreter: Interpreter = printScriptInterpreter
)

data class Compiler(
  val config: Config
) {
  fun execute(content: ContentManager): Result<Unit> {
    val cursor = content.openStream()
    val tokens = config.lexer.tokenize(cursor)
    val environment = Environment()

    for (parseResult in config.parser.parse(tokens)) {
      val statement = when (parseResult) {
        is Failure -> return Failure(parseResult.msg, parseResult.type)
        is Success -> parseResult.data
      }

      for (semanticResult in config.semantic.analyze(sequenceOf(statement))) {
        if (semanticResult is Failure) {
          return Failure(semanticResult.msg, semanticResult.type)
        }
      }

      val interpretResult = config.interpreter.interpret(statement, environment)
      if (interpretResult is Failure) {
        return Failure(interpretResult.msg, interpretResult.type)
      }
    }
    return Success("ok", Unit)
  }

  fun validate(content: ContentManager): Result<Unit> {
    val cursor = content.openStream()
    val tokens = config.lexer.tokenize(cursor)

    for (parseResult in config.parser.parse(tokens)) {
      val statement = when (parseResult) {
        is Failure -> return Failure(parseResult.msg, parseResult.type)
        is Success -> parseResult.data
      }

      for (semanticResult in config.semantic.analyze(sequenceOf(statement))) {
        if (semanticResult is Failure) {
          return Failure(semanticResult.msg, semanticResult.type)
        }
      }
    }
    return Success("Code is valid", Unit)
  }

  fun compile(content: ContentManager) {
    val result = execute(content)
    when (result) {
      is Failure -> println("ERROR: ${result.msg}")
      is Success -> {}
    }
  }

  fun compile(path: String) = compile(FileContent(path))
}

fun main() {
  CLISystem.run()
}
