package org.lexer

interface Analizer {
  fun analizeSingle(content: String) : Token; //analizo de un token a token
}

class StdAnalizer : Analizer {
  override fun analizeSingle(contet: String) : Token {
    TODO("Not implemented yet");
  }
}
