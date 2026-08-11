package org.lexer

import org.config.PlusSign
import org.config.MinusSign
import org.config.DivisionSign
import org.config.MultiplicationSign
import org.utils.Position

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
class RegexSplitter : Splitter {
  private fun getRegex() : Regex {
    
  } 
  // me deberia devolver el solo la siguiente porcion a analizar
  override fun split(content: String) : Sequence<Coincidence> {
    return regex
    .findAll(content)
    .map { match -> Coincidence(match.value, match.range.first) }
  }
}

class CharSplitter : Splitter {
  override fun split(content: String) : Sequence<Coincidence> {
    val binaryOperators = setOf(
      PlusSign,
      MinusSign,
      DivisionSign,
      MultiplicationSign
    )
    
    TODO("not implemented yet!")
  }
}
