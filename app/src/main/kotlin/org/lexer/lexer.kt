package org.lexer

import org.utils.Position
import org.utils.Result

// necesito el content porque asi puedo saber donde quede la ultima vez
// que saque un token.
class Lexer (
  val formater: Formater, 
  val content: ContentManager
) {
  fun getTokens() : List<Token> {
    return content
      .getLines()
      .withIndex()
      .map { (index, line)  -> index to formater.format(line) }
      .flatMap { (index, seq) -> 
        seq.map { match ->
          Token(
            TokenIdentifier.type(match.value), 
            Position(index+1, match.range.first), 
            match.value
          )
        }
      }
      .toList();
  }
};

class LexerBuilder {

  var formater: Formater = StdFormater()
  var content: ContentManager? = null

  fun setFormater(f: Formater) = apply { formater = f; }
  fun setContent(c: ContentManager) = apply { content = c; }
  fun build() : Lexer {
    return Lexer(
      formater, 
      content?: error("content not set")
      );
  }

}
