package org

import org.lexer.LexerBuilder
import org.lexer.StdFormater
import org.lexer.StrContent

fun main() {
  val lexer = LexerBuilder()
    .setContent(StrContent("let name: string = \"John\";"))
    .build()
  print(lexer.getTokens());
}
