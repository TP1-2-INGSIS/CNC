package cnc.config

val expressionBuilder = ExpressionBuilder(mapOf(
  CncPatterns.NUMBER to { token: Token -> NumberLiteral(token.text.toDouble()) },
  CncPatterns.STRING to { token: Token -> StringLiteral(token.text.removeSurrounding("\"")) },
  CncPatterns.IDENTIFIER to { token -> Identifier(token.text) }
))
