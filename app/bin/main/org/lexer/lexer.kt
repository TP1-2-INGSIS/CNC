package org.lexer

import org.utils.Position

// necesito el content porque asi puedo saber donde quede la ultima vez
// que saque un token.
class Lexer (
  val formater: Formater, 
  val analyzer: Analyzer,
  val content: ContentManager
) {
  fun getTokens() : List<Token> {
    var tokens = mutableListOf<Token>()
    while(!content.allRead()) {
      val formated : List<String> = formater.format(content.getNextLine()) // va cargando una linea a la vez en memoria
      tokens.addAll(analyzer.analyze(formated))
    }
    return tokens
  }
};

class LexerBuilder {

  var formater: Formater? = null
  var analyzer: Analyzer? = null
  var content: ContentManager? = null

  fun setFormater(f: Formater) = apply { formater = f; }
  fun setAnalyzer(a: Analyzer) = apply { analyzer = a; }
  fun setContent(c: ContentManager) = apply { content = c; }
  fun build() : Lexer {
    return Lexer(
      formater?: error("formater not set"), 
      analyzer?: error("analyzer not set"),
      content?: error("content not set")
      );
  }

}
