package cnc.parser

import cnc.token.Token
import cnc.token.TokenDefinition

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

