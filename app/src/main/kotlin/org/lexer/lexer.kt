package org.lexer

class Lexer (
  val formater: Formater, 
  val analizer: Analizer
) {
  // tambien hay que pensar en que se pueda hacer lazy
  fun getTokens(content: String) { TODO("Not implemented yet") }
};
