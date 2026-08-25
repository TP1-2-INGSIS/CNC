package cnc.config

import cnc.token.Token
import cnc.token.TokenType
import cnc.token.RegexTokenDef
import cnc.token.SymbolTokenDef
import cnc.token.TokenDefinition

import cnc.ast.ExpressionBuilder
import cnc.ast.OperatorDef
import cnc.ast.Associativity
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

import cnc.semantic.BinaryOpResolver
import cnc.semantic.TypeResolvers
import cnc.semantic.SymbolTable

import cnc.lexer.Lexer
import cnc.lexer.rules.StandardRules
import cnc.lexer.rules.TrieRule

// CNC DOMAIN DEFINITIONS ============================================================

object CncKeywords {
  val LET = SymbolTokenDef("let", "let")
  val TYPE_STRING = SymbolTokenDef("string_type", "string")
  val TYPE_NUMBER = SymbolTokenDef("number_type", "number")

  val types: List<TokenDefinition> = listOf(TYPE_STRING, TYPE_NUMBER)

  val all = mapOf(
    "let" to TokenType.KEYWORD,
    "string" to TokenType.VARIABLE_TYPE,
    "number" to TokenType.VARIABLE_TYPE
  )
}

object CncSymbols {
  val PLUS = SymbolTokenDef("plus", "+")
  val MINUS = SymbolTokenDef("minus", "-")
  val DIVISION = SymbolTokenDef("division", "/")
  val MULTIPLICATION = SymbolTokenDef("multiplication", "*")
  val EQUALS = SymbolTokenDef("equals", "==")
  val EXPONENT = SymbolTokenDef("exponent", "**")
  val SEMICOLON = SymbolTokenDef("semicolon", ";")
  val COLON = SymbolTokenDef("colon", ":")
  val ASSIGN = SymbolTokenDef("assign", "=")
  val OPEN_PAREN = SymbolTokenDef("open_paren", "(")
  val CLOSE_PAREN = SymbolTokenDef("close_paren", ")")

  val all = mapOf(
    "+" to TokenType.OPERATOR,
    "-" to TokenType.OPERATOR,
    "/" to TokenType.OPERATOR,
    "*" to TokenType.OPERATOR,
    "==" to TokenType.OPERATOR,
    "**" to TokenType.OPERATOR,
    ";" to TokenType.SYMBOL,
    ":" to TokenType.SYMBOL,
    "=" to TokenType.SYMBOL,
    "(" to TokenType.SYMBOL,
    ")" to TokenType.SYMBOL
  )
}

object CncPatterns {
  val IDENTIFIER = RegexTokenDef("identifier", "[a-zA-Z_][a-zA-Z0-9_]*")
  val NUMBER = RegexTokenDef("number_exp", "[0-9]+")
  val STRING = RegexTokenDef("string_exp", "\".*?\"")
}

// LEXER CONFIGURATION ===============================================================

val printScriptRules = listOf(
  StandardRules.whitespace(),
  StandardRules.doubleQuotedString(TokenType.STRING),
  StandardRules.integerNumber(TokenType.NUMBER),
  StandardRules.standardIdentifier(keywords = CncKeywords.all),
  TrieRule(CncSymbols.all)
)

val printScriptLexer = Lexer(printScriptRules)

// GRAMMAR AND STATEMENTS =======================================================

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
      CncSymbols.DIVISION,
      CncSymbols.OPEN_PAREN,
      CncSymbols.CLOSE_PAREN
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
      CncSymbols.DIVISION,
      CncSymbols.OPEN_PAREN,
      CncSymbols.CLOSE_PAREN
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

// EXPRESSIONS BUILDER ==========================================================
val expressionBuilder = ExpressionBuilder(
  recipes = mapOf(
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
// BINARY TYPE RULES ============================================================

val binaryTypeRules: Map<String, BinaryOpResolver> = mapOf(
  "+"  to TypeResolvers.additionOrConcat,
  "-"  to TypeResolvers.numericOnly("-"),
  "*"  to TypeResolvers.numericOnly("*"),
  "/"  to TypeResolvers.numericOnly("/"),
)

// SYMBOL TABLE =================================================================

val symbolTable = SymbolTable(validTypes = setOf("number", "string"))
