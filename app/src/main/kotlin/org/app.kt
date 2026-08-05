package org

import org.lexer.Lexer
import org.lexer.StdFormater
import org.lexer.StdAnalizer

fun main() {
  print(Lexer(StdFormater(), StdAnalizer()).getTokens("hola"));
}
