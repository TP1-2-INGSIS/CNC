package cnc.config

import cnc.token.Token
import cnc.token.TokenType
import cnc.token.RegexTokenDef
import cnc.token.SymbolTokenDef
import cnc.token.TokenDefinition

import cnc.ast.NumberLiteral
import cnc.ast.StringLiteral
import cnc.ast.Identifier
import cnc.ast.Declaration
import cnc.ast.Assignment
import cnc.ast.Call
import cnc.ast.BinaryExpression

import cnc.interpreter.Interpreter
import cnc.interpreter.DeclarationEvaluator
import cnc.interpreter.AssignmentEvaluator
import cnc.interpreter.CallEvaluator
import cnc.interpreter.NumberLiteralEvaluator
import cnc.interpreter.StringLiteralEvaluator
import cnc.interpreter.IdentifierEvaluator
import cnc.interpreter.BinaryExpressionEvaluator
import cnc.interpreter.StandardBinaryOperations
import cnc.interpreter.BinaryOperation

import cnc.parser.Parser
import cnc.parser.expression.ExpressionBuilder
import cnc.parser.expression.OperatorDef
import cnc.parser.expression.Associativity
import cnc.parser.rule.StandardStatementRules

import cnc.semantic.BinaryOpResolver
import cnc.semantic.TypeResolvers
import cnc.semantic.SymbolTable
import cnc.semantic.DefaultSemanticContext

import cnc.lexer.Lexer
import cnc.lexer.rules.StandardRules
import cnc.lexer.rules.TrieRule

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

val printScriptRules = listOf(
  StandardRules.whitespace(),
  StandardRules.doubleQuotedString(TokenType.STRING),
  StandardRules.integerNumber(TokenType.NUMBER),
  StandardRules.standardIdentifier(keywords = CncKeywords.all),
  TrieRule(CncSymbols.all)
)

val printScriptLexer = Lexer(printScriptRules)

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

val printScriptParser = Parser(
  rules = StandardStatementRules.printScript10,
  expressionParser = expressionBuilder
)

val printScriptStatementEvaluators = mapOf(
  Declaration::class to DeclarationEvaluator(),
  Assignment::class to AssignmentEvaluator(),
  Call::class to CallEvaluator()
)

val printScriptBinaryOperations: Map<String, BinaryOperation> = mapOf(
  CncSymbols.PLUS.symbols.first() to BinaryOperation(StandardBinaryOperations::add),
  CncSymbols.MINUS.symbols.first() to BinaryOperation(StandardBinaryOperations::subtract),
  CncSymbols.MULTIPLICATION.symbols.first() to BinaryOperation(StandardBinaryOperations::multiply),
  CncSymbols.DIVISION.symbols.first() to BinaryOperation(StandardBinaryOperations::divide)
)

val printScriptExpressionEvaluators = mapOf(
  NumberLiteral::class to NumberLiteralEvaluator(),
  StringLiteral::class to StringLiteralEvaluator(),
  Identifier::class to IdentifierEvaluator(),
  BinaryExpression::class to BinaryExpressionEvaluator(printScriptBinaryOperations)
)

val printScriptInterpreter = Interpreter(
  statementEvaluators = printScriptStatementEvaluators,
  expressionEvaluators = printScriptExpressionEvaluators
)

val binaryTypeRules: Map<String, BinaryOpResolver> = mapOf(
  "+" to TypeResolvers.additionOrConcat,
  "-" to TypeResolvers.numericOnly("-"),
  "*" to TypeResolvers.numericOnly("*"),
  "/" to TypeResolvers.numericOnly("/"),
)

val symbolTable = SymbolTable(validTypes = setOf("number", "string"))

val semanticContext = DefaultSemanticContext(symbolTable, binaryTypeRules)
