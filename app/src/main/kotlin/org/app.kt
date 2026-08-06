package org

import org.lexer.StrContent
import org.lexer.RegexSplitter
import org.lexer.Lexer

fun main() {
  print(Lexer().getTokens(StrContent("let name: string = \"John\";")));
}
