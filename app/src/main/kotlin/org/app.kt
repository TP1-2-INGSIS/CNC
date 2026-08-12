package org

import org.lexer.Lexer
import org.lexer.StrContent
import org.parser.Parser

fun main() {
  generateSequence { readLine() }
  .map { StrContent(it) }
  .map { Lexer.getTokens(it) }
  .flatMap { Parser.getAST(it) }
  .forEach { println(it) }
}
