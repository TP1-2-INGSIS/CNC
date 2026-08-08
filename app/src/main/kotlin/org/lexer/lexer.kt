package org.lexer

import org.utils.Position
import org.utils.Result
import org.config.Token
import org.config.TokenIdentifier

// necesito el content porque asi puedo saber donde quede la ultima vez
// que saque un token.
//
// Lo hice object porque es indiferente tener una clase si solo vamos a
// tener un lexer en todo el compiler. De paso nos ahorramos de hacer
// un Lexer() y tener que ponerle esos parentesis feos.
//
// TODO: Deberiamos hacer una interfaz? interface Lexer y hacer una impl?
// yo creo que no, pero la dejo picando
object Lexer {
  val splitter: Splitter = RegexSplitter()
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
