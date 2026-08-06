package org.lexer

import org.utils.Position

interface Analyzer {
  fun analyze(content: String) : Token; //analizo de un token a token>
}

class StdAnalyzer : Analyzer {
  override fun analyze(content: String) : Token {
    return Token(
      type= TokenIdentifier.type(content),
      pos= Position(0,0),
      text= content
    )
  }
}
