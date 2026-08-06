package org.lexer

import org.utils.Position

interface Formater {
  fun format(content: String) : Sequence<MatchResult>;
}

// habria que cambiarlo, por ahora funciona
// pero no es nada escalable. Tener que definir todo con regex es muy poco escalable
// segun chat
class StdFormater : Formater {
  val regex = """([a-zA-Z_][a-zA-Z0-9_]*)|(=|\*|\\|;)|([0-9_]+)|(\".*\")""".toRegex()
  // me deberia devolver el solo la siguiente porcion a analizar
  override fun format(content: String) : Sequence<MatchResult> {
    return regex.findAll(content);
  }
}
