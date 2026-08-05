package org.lexer

import org.utils.Position

interface Formater {
  fun format(content: String) : List<String>;
}

class StdFormater : Formater {
  // me deberia devolver el solo la siguiente porcion a analizar
  override fun format(content: String) : List<String> {
    return content.split(" ") // por ahora solo separo entre espacios
  }
}
