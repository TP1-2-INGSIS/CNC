package cnc.config

import cnc.ast.Associativity
import cnc.ast.ExpressionBuilder
import cnc.ast.Identifier
import cnc.ast.NumberLiteral
import cnc.ast.OperatorDef
import cnc.ast.StringLiteral

import cnc.token.Token

/**
 * ExpressionBuilder de PrintScript: recetas para literales/identificadores y la
 * tabla de operadores con su precedencia (usada por el Pratt parser del AST).
 */
val expressionBuilder = ExpressionBuilder(
  recipes = mapOf(
    CncPatterns.NUMBER to { token: Token -> NumberLiteral(token.text.toDouble()) },
    CncPatterns.STRING to { token: Token -> StringLiteral(token.text.removeSurrounding("\"")) },
    CncPatterns.IDENTIFIER to { token: Token -> Identifier(token.text) }
  ),
  operators = listOf(
    OperatorDef(CncSymbols.PLUS, precedence = 1),
    OperatorDef(CncSymbols.MINUS, precedence = 1),
    OperatorDef(CncSymbols.MULTIPLICATION, precedence = 2),
    OperatorDef(CncSymbols.DIVISION, precedence = 2),
    OperatorDef(CncSymbols.EXPONENT, precedence = 3, associativity = Associativity.RIGHT)
  )
)
