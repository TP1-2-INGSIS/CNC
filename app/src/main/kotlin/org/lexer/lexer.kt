package org.lexer

import org.utils.Position
import org.utils.Result
import org.config.Token
import org.config.TokenIdentifier

// necesito el content porque asi puedo saber donde quede la ultima vez
// que saque un token.
class Lexer {
  val splitter: Splitter = RegexSplitter() // no hace falta exponerlo al cliente
  fun getTokens(content: ContentManager) : Sequence<Token> {
    return content
      .getLines()
      .withIndex()
      .map { (index, line)  -> index to splitter.split(line) }
      .flatMap { (index, seq) -> 
        seq.map { match ->
          Token(
            TokenIdentifier.type(match.value), 
            Position(index+1, match.range.first), 
            match.value
          )
        }
      }
  }
};
