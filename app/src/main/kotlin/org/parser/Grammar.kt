package org.parser

import org.config.TokenDefinition
import org.config.Token
import org.config.VariableDefinition
import org.config.IdentifierDefinition
import org.config.AssignDefinition
import org.config.NumberExpressionDefinition
import org.config.StringExpressionDefinition
import org.config.TerminationDefinition
import org.config.TypeDefinition
import org.config.TokenDefinitionProvider
import org.config.TokenType


interface GrammarStrategy {
  fun eval(token: Token): Boolean
}

class IsStrat(val definition: TokenDefinition) : GrammarStrategy {
  override fun eval(token: Token): Boolean = definition.match(token.text)
}

class AnyStrat(val strats: List<GrammarStrategy>) : GrammarStrategy {
  override fun eval(token: Token): Boolean = strats.any { it.eval(token) }
}

class AnyTypeVariableStrat : GrammarStrategy {
  override fun eval(token: Token): Boolean = TokenDefinitionProvider.getDefinitions(TokenType.VARIABLE_TYPE)!!.any { it.match(token.text) }
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

// gramatica es el formato que tiene que cumplir el token para pertenecer al statement planteado (rule set)
val VariableDeclaration = Grammar(
  tag = "VariableDeclaration",
  sequence = listOf(
    IsStrat(VariableDefinition),
    IsStrat(IdentifierDefinition),
    IsStrat(TypeDefinition),
    AnyTypeVariableStrat(),
    IsStrat(AssignDefinition),
    AnyStrat(listOf(
      IsStrat(NumberExpressionDefinition),
      IsStrat(StringExpressionDefinition)
    )),
    IsStrat(TerminationDefinition)
  ),
  build = { tokens ->
    Declaration(
      name = tokens[1].text,
      type = tokens[3].text,
      value = expressionBuilder.build(tokens[5])
    )
  }
)

// =======================================================================================
// ExpressionBuilder 
// -> clase que se encargue de construir las expressiones a partir de:
// - Token
// - TokenDefinition
// TODO: mover a una carpeta decente



fun buildExpression(token: Token): Expression {
  return when {
    NumberExpressionDefinition.match(token.text) -> NumberLiteral(token.text.toDouble())
    StringExpressionDefinition.match(token.text) -> StringLiteral(token.text.removeSurrounding("\""))
    IdentifierDefinition.match(token.text) -> Identifier(token.text)
    else -> error("Unknown expression: ${token.text}")
  }
}

val grammars: List<Grammar> = listOf(
  VariableDeclaration
)
