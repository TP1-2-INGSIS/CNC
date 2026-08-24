package cnc.ast

import cnc.token.Token
import cnc.token.TokenDefinition

// TODO: Crear evaluators para cada statement
// el Parser puede crear los AST conociendolos
// y como comparte con el Interpreter no hay problema
// mientras tengamos los Evaluators de cada statement.

sealed interface Statement

data class Declaration(
  val name: String,
  val type: String,
  val value: Expression?,
) : Statement

data class Assignment(
  val target: String,
  val value: Expression,
) : Statement

data class Call(
  val function: String,
  val arguments: List<Expression>,
) : Statement

sealed interface Expression

data class NumberLiteral(
  val value: Double,
) : Expression

data class StringLiteral(
  val value: String,
) : Expression

data class Identifier(
  val name: String,
) : Expression

data class BinaryExpression(
  val left: Expression,
  val operator: String,
  val right: Expression,
) : Expression

class ExpressionBuilder(
  private val recipes: Map<TokenDefinition, (Token) -> Expression>,
) {
  fun build(token: Token): Expression {
    val (_, builder) =
      recipes.entries.first { (definition, _) ->
        definition.match(token.text)
      }
    return builder(token)
  }

  fun build(tokens: List<Token>): Expression = build(tokens.first())
}
