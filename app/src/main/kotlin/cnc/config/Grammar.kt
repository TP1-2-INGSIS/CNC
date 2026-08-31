package cnc.config

import cnc.ast.ExpressionBuilder
import cnc.ast.NumberLiteral
import cnc.ast.StringLiteral
import cnc.ast.Identifier
import cnc.ast.Declaration
import cnc.ast.Assignment

import cnc.parser.Grammar
import cnc.parser.ExpressionStrat
import cnc.parser.IsStrat
import cnc.parser.AnyStrat
import cnc.parser.AnyOfTypeStrat

import cnc.token.Token
import cnc.token.TokenType
import cnc.token.RegexTokenDef
import cnc.token.SymbolTokenDef
import cnc.token.TokenDefinition

val VariableDeclaration = Grammar(
  tag = "VariableDeclaration",
  sequence = listOf(
    IsStrat(CncKeywords.LET),              // segments[0] = [let]
    IsStrat(CncPatterns.IDENTIFIER),       // segments[1] = [x]
    IsStrat(CncSymbols.COLON),             // segments[2] = [:]
    AnyOfTypeStrat(CncKeywords.types),     // segments[3] = [number]
    IsStrat(CncSymbols.ASSIGN),            // segments[4] = [=]
    ExpressionStrat(listOf(
      CncPatterns.NUMBER,
      CncPatterns.STRING,
      CncPatterns.IDENTIFIER,
      CncSymbols.PLUS,
      CncSymbols.MINUS,
      CncSymbols.MULTIPLICATION,
      CncSymbols.DIVISION
    )),                                    // segments[5] = [2, *, (, x, +, 3, )]
    IsStrat(CncSymbols.SEMICOLON)          // segments[6] = [;]
  ),
  build = { segments ->
    Declaration(
      name = segments[1].first().text,
      type = segments[3].first().text,
      value = expressionBuilder.build(segments[5])
    )
  }
)

val VariableAssignment = Grammar(
  tag = "VariableAssignment",
  sequence = listOf(
    IsStrat(CncPatterns.IDENTIFIER),       // segments[0] = [x]
    IsStrat(CncSymbols.ASSIGN),            // segments[1] = [=]
    ExpressionStrat(listOf(
      CncPatterns.NUMBER,
      CncPatterns.STRING,
      CncPatterns.IDENTIFIER,
      CncSymbols.PLUS,
      CncSymbols.MINUS,
      CncSymbols.MULTIPLICATION,
      CncSymbols.DIVISION
    )),                                    // segments[2] = [2, *, (, x, +, 3, )]
    IsStrat(CncSymbols.SEMICOLON)          // segments[3] = [;]
  ),
  build = { segments ->
    Assignment(
      target = segments[0].first().text,
      value = expressionBuilder.build(segments[2])
    )
  }
)

val terminators: List<TokenDefinition> = listOf(
  CncSymbols.SEMICOLON
)

val grammars = listOf(
  VariableDeclaration,
  VariableAssignment
)

