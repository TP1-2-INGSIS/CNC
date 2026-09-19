package cnc.config

import cnc.ast.Identifier
import cnc.ast.NumberLiteral
import cnc.ast.StringLiteral
import cnc.parser.Parser
import cnc.parser.expression.Associativity
import cnc.parser.expression.ExpressionBuilder
import cnc.parser.expression.OperatorDef
import cnc.parser.rule.StandardStatementRules
import cnc.token.Token

val expressionBuilder = ExpressionBuilder(
  recipes = mapOf(
    cnc.token.SymbolTokenDef("true", "true") to { _ -> cnc.ast.BooleanLiteral(true) },
    cnc.token.SymbolTokenDef("false", "false") to { _ -> cnc.ast.BooleanLiteral(false) },
    CncPatterns.NUMBER to { token: Token -> NumberLiteral(token.text.toDouble()) },
    CncPatterns.STRING to { token: Token -> StringLiteral(token.text.removeSurrounding("\"")) },
    CncPatterns.IDENTIFIER to { token -> Identifier(token.text) }
  ),
  operators = listOf(
    OperatorDef(CncSymbols.PLUS, precedence = 1),
    OperatorDef(CncSymbols.MINUS, precedence = 1),
    OperatorDef(CncSymbols.MULTIPLICATION, precedence = 2),
    OperatorDef(CncSymbols.DIVISION, precedence = 2),
    OperatorDef(CncSymbols.EXPONENT, precedence = 3, associativity = Associativity.RIGHT)
  ),
  groupOpen = CncSymbols.OPEN_PAREN,
  groupClose = CncSymbols.CLOSE_PAREN
)

val printScriptParser = Parser(
  rules = StandardStatementRules.printScript10,
  expressionParser = expressionBuilder
)
