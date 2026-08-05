package org

import org.lexer.LexerBuilder
import org.lexer.StdFormater
import org.lexer.StdAnalyzer
import org.lexer.StrContent

fun main() {
  val lexer = LexerBuilder()
    .setFormater(StdFormater())
    .setAnalyzer(StdAnalyzer())
    .setContent(StrContent(
    """
    let name: String = "John";
    """
    ))
    .build()
  print(lexer.getTokens());
}
