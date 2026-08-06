package org.lexer

import org.utils.Position
import org.utils.Result

// necesito el content porque asi puedo saber donde quede la ultima vez
// que saque un token.
class Lexer (
  val formater: Formater, 
  val analyzer: Analyzer,
  val content: ContentManager
) {
  fun getTokens() : List<Token> {
    return formater.format(content.getNextLine())
      .asSequence()
      //.map {str -> Token(TokenType.INVALID, Position(0,0), str)}
      .map {token -> analyzer.analyze(token) }
      .toList();
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
