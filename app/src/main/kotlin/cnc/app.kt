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
    val parsedStatements = mutableListOf<Statement>()
    for (result in config.parser.parse(tokens)) {
      when (result) {
        is Failure -> return Failure<Unit>(result.msg, result.type)
        is Success -> parsedStatements.add(result.data)
      }
    }
    val validatedStatements = mutableListOf<Statement>()
    for (result in config.semantic.analyze(parsedStatements.asSequence())) {
      when (result) {
        is Failure -> return Failure<Unit>(result.msg, result.type)
        is Success -> validatedStatements.add(result.data)
      }
    }
    val environment = Environment()
    for (result in config.interpreter.interpretAll(validatedStatements, environment)) {
      when (result) {
        is Failure -> return Failure<Unit>(result.msg, result.type)
        is Success -> { /* Statement executed successfully */ }
      }
    }
    return Success("ok", Unit)
  }

  fun validate(content: ContentManager): Result<Unit> {
    val cursor = content.openStream()
    val tokens = config.lexer.tokenize(cursor)
    val parsedStatements = mutableListOf<Statement>()
    for (result in config.parser.parse(tokens)) {
      when (result) {
        is Failure -> return Failure<Unit>(result.msg, result.type)
        is Success -> parsedStatements.add(result.data)
      }
    }
    for (result in config.semantic.analyze(parsedStatements.asSequence())) {
      when (result) {
        is Failure -> return Failure<Unit>(result.msg, result.type)
        is Success -> { /* Statement validated successfully */ }
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
