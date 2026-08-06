package org

import org.lexer.StrContent
import org.lexer.RegexSplitter
import org.lexer.Lexer

fun main() {
  Lexer().getTokens(StrContent("let name: string = \"John\";")).forEach { print(it) };
}
