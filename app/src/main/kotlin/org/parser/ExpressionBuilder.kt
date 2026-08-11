package org.parser

import org.config.Token
import org.config.TokenDefinition
import org.config.NumberExpressionDefinition
import org.config.StringExpressionDefinition
import org.config.IdentifierDefinition

class ExpressionBuilder (
  private val recipes: Map<TokenDefinition, (Token) -> Expression>
) {
  fun build(token: Token) : Expression {
    val (_, builder) = recipes.entries.first { (definition, _) -> 
      definition.match(token.text)
    } 
    return builder(token)
  }
}

val expressionBuilder = ExpressionBuilder(mapOf(
  NumberExpressionDefinition to { token: Token -> NumberLiteral(token.text.toDouble()) },
  StringExpressionDefinition to { token: Token -> StringLiteral(token.text.removeSurrounding("\"")) },
  IdentifierDefinition to { token -> Identifier(token.text) }
))


