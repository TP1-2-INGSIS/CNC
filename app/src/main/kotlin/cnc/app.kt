package cnc

import cnc.config.*
import cnc.lexer.Lexer
import cnc.common.FileContent
import cnc.common.ContentManager
import cnc.parser.Parser

import cnc.cli.CommandSystem
import cnc.cli.command.Command
import cnc.cli.command.GccCommand

import cnc.common.Result
import cnc.common.Failure
import cnc.common.Success
import cnc.common.ErrorType

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
  CommandSystem(mapOf(
    GccCommand.tag to GccCommand
  )).run()
}
