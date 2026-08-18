package cnc.parser

import cnc.token.Token
import cnc.token.TokenType
import cnc.token.TokenDefinition
import cnc.token.TokenDefinitionProvider 

import cnc.ast.Statement

interface GrammarStrategy {
  fun eval(token: Token): Boolean
}

// Definiciones especificas tambien son acoplamiento de conocimiento
// especifico de lo que estamos haciendo, o capaz no, podria ser
// que el parser o en realidad el grammar te de una suite de strats
// por default pero vos puedas implementar la tuya propia.
// TODO: Definirlo y plantearlo mejor
class IsStrat(val definition: TokenDefinition) : GrammarStrategy {
  override fun eval(token: Token): Boolean = definition.match(token.text)
}

class AnyStrat(val strats: List<GrammarStrategy>) : GrammarStrategy {
  override fun eval(token: Token): Boolean = strats.any { it.eval(token) }
}
class AnyTypeVariableStrat(
  val tokenDefProvider: TokenDefinitionProvider 
) : GrammarStrategy {
  override fun eval(token: Token): Boolean = tokenDefProvider.getValue(TokenType.VARIABLE_TYPE)!!.any { it.match(token.text) }
}

data class Grammar(
  val tag: String,
  val sequence: List<GrammarStrategy>,
  val build: (List<Token>) -> Statement
) {
  fun matches(tokens: List<Token>): Boolean {
    if (tokens.size < sequence.size) return false
    return sequence.zip(tokens).all { (strat, token) -> strat.eval(token) }
  }
}

