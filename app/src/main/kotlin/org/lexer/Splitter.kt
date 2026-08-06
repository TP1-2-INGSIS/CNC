package org.lexer

import org.utils.Position

interface Splitter {
  fun split(content: String) : Sequence<MatchResult>;
}

// habria que cambiarlo, por ahora funciona
// pero no es nada escalable. Tener que definir todo con regex es muy poco escalable
// segun chat
class RegexSplitter : Splitter {
  val regex = """([a-zA-Z_][a-zA-Z0-9_]*)|(=|\*|\\|;|:)|([0-9_]+)|(\".*\")""".toRegex()
  // me deberia devolver el solo la siguiente porcion a analizar
  override fun split(content: String) : Sequence<MatchResult> {
    return regex.findAll(content);
  }
}
