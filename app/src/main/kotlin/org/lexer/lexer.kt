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
    return content
      .getLines()
      .flatMap { line  -> formater.format(line) }
      .map { match -> 
        Token(
          TokenIdentifier.type(match.value), 
          Position(0,match.range.first), 
          match.value
        )
      }
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
