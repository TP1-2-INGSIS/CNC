package cnc.lexer

import cnc.token.TokenDefinitionProvider
import cnc.token.RegexTokenDef
import cnc.common.Position

data class Coincidence(
  val text: String,
  val index: Int
)

interface Splitter {
  fun split(content: String) : Sequence<Coincidence>;
}

// habria que cambiarlo, por ahora funciona
// pero no es nada escalable. Tener que definir todo con 
// regex es muy poco escalable segun chat
class RegexSplitter(val tokenDefs: TokenDefinitionProvider) : Splitter {

private fun getRegex(): Regex = tokenDefs.getTypes()
    .joinToString("|") { type ->
        val group = tokenDefs.getValue(type)!!
            .flatMap { def ->
                def.symbols.map { symbol ->
                    // Si es un RegexTokenDef, dejamos el patrón como está.
                    // Si es un TokenDef común, escapamos los caracteres especiales (+, -, =, etc.)
                    if (def is RegexTokenDef) symbol else Regex.escape(symbol)
                }
            }
            .joinToString("|")
        "($group)"
    }.toRegex()

  // me deberia devolver el solo la siguiente porcion a analizar
  override fun split(content: String) : Sequence<Coincidence> {
    return getRegex()
    .findAll(content)
    .map { match -> Coincidence(match.value, match.range.first) }
  }
}

class CharSplitter : Splitter {
  override fun split(content: String) : Sequence<Coincidence> {
    TODO("not implemented yet!")
  }
}
