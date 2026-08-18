package cnc.lexer

import cnc.common.Position
import cnc.common.ContentManager
import cnc.token.Token
import cnc.token.TokenType
import cnc.token.TokenDefinitionProvider

// necesito el content porque asi puedo saber donde quede la ultima vez
// que saque un token.
//
// Lo hice object porque es indiferente tener una clase si solo vamos a
// tener un lexer en todo el compiler. De paso nos ahorramos de hacer
// un Lexer() y tener que ponerle esos parentesis feos.
//
// TODO: Deberiamos hacer una interfaz? interface Lexer y hacer una impl?
// yo creo que no, pero la dejo picando
class Lexer(
  val tokenDefs : TokenDefinitionProvider
) {
  val splitter: Splitter = RegexSplitter(tokenDefs)
  fun getTokens(line: String, row: Int): Sequence<Token> {
    return splitter.split(line).map { (match, col) ->
          Token(
            tokenDefs.type(match),
            Position(row, col),
            match
          )
        }
  }
}
