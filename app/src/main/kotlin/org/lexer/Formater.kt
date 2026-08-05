package org.lexer

interface Formater {
  fun format(content: String) : String;
}

class StdFormater : Formater {
  override fun format(content: String) : String {
      return content;
  }
}
