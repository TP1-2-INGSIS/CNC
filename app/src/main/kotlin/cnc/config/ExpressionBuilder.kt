package cnc.config

import cnc.ast.ExpressionBuilder
import cnc.ast.NumberLiteral
import cnc.ast.StringLiteral
import cnc.ast.Identifier

import cnc.token.Token

import cnc.definition.*

val expressionBuilder = ExpressionBuilder(mapOf(
  NumberExpressionDefinition to { token: Token -> NumberLiteral(token.text.toDouble()) },
  StringExpressionDefinition to { token: Token -> StringLiteral(token.text.removeSurrounding("\"")) },
  IdentifierDefinition to { token -> Identifier(token.text) }
))
