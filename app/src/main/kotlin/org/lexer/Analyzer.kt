package org.lexer

interface Analyzer {
  fun analyze(content: List<String>) : List<Token>; //analizo de un token a token>
}

class StdAnalyzer : Analyzer {
  override fun analyze(content: List<String>) : List<Token> {
    print(content)
    TODO("Not finished!")
  }
}
