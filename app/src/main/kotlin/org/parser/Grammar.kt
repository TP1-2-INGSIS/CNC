package org.parser

import org.config.TokenDefinition
import org.config.VariableDefinition
import org.config.IdentifierDefinition
import org.config.AssignDefinition
import org.config.NumberExpressionDefinition
import org.config.StringExpressionDefinition
import org.config.TerminationDefinition

interface GrammarStrategy {
  fun eval(token: TokenDefinition) : Boolean;
}

class IsStrat(val _token: TokenDefinition) : GrammarStrategy {
  override fun eval(token: TokenDefinition) : Boolean = _token == token 
}

class AnyStrat(val strats: List<GrammarStrategy>) : GrammarStrategy {
  override fun eval(token: TokenDefinition) : Boolean = strats.any { it.eval(token) }
}

data class Grammar(
  val tag: String,
  val sequence: Sequence<GrammarStrategy>
)

// gramatica es el formato que tiene que cumplir (rule set)
val VariableDeclaration = Grammar("VariableDeclaration", sequenceOf(
  IsStrat(VariableDefinition),
  IsStrat(IdentifierDefinition),
  IsStrat(AssignDefinition),
  AnyStrat(listOf(IsStrat(NumberExpressionDefinition), IsStrat(StringExpressionDefinition))),
  IsStrat(TerminationDefinition)
))
