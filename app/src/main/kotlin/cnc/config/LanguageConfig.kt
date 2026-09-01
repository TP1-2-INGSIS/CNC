package cnc.config

import cnc.token.Token
import cnc.token.TokenType
import cnc.token.RegexTokenDef
import cnc.token.SymbolTokenDef
import cnc.token.TokenDefinition

import cnc.ast.ExpressionBuilder
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
import cnc.interpreter.NumberOperations
import cnc.interpreter.StandardBinaryOperations
import cnc.interpreter.BinaryOperation
import cnc.ast.*
import cnc.common.*

import cnc.parser.Grammar
import cnc.parser.Step
import cnc.parser.ExpressionStrat
import cnc.parser.IsStrat
import cnc.parser.AnyStrat
import cnc.parser.AnyOfTypeStrat

import cnc.semantic.BinaryOpResolver
import cnc.semantic.TypeResolvers
import cnc.semantic.SymbolTable
import cnc.semantic.DefaultSemanticContext

import cnc.lexer.Lexer
import cnc.lexer.rules.StandardRules
import cnc.lexer.rules.TrieRule

// =============================================================================
// CNC DOMAIN DEFINITIONS
// =============================================================================

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

// =============================================================================
// LEXER CONFIGURATION
// =============================================================================

val printScriptRules = listOf(
  StandardRules.whitespace(),
  StandardRules.doubleQuotedString(TokenType.STRING),
  StandardRules.integerNumber(TokenType.NUMBER),
  StandardRules.standardIdentifier(keywords = CncKeywords.all),
  TrieRule(CncSymbols.all)
)

val printScriptLexer = Lexer(printScriptRules)

// =============================================================================
// EXPRESSIONS
// =============================================================================

val expressionTokens = listOf(
  CncPatterns.NUMBER,
  CncPatterns.STRING,
  CncPatterns.IDENTIFIER,
  CncSymbols.PLUS,
  CncSymbols.MINUS,
  CncSymbols.MULTIPLICATION,
  CncSymbols.DIVISION,
  CncSymbols.OPEN_PAREN,
  CncSymbols.CLOSE_PAREN
)

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

// =============================================================================
// STATEMENT DEFINITIONS
// =============================================================================

val DeclarationDef = StatementDef(
  tag = "Declaration",
  fields = mapOf(
    "name" to FieldType.TEXT,
    "type" to FieldType.TEXT,
    "value" to FieldType.EXPRESSION
  ),
  semanticCheck = { fields, ctx ->
    val name = fields.text("name")
    val type = fields.text("type")
    val value = fields.expression("value")

    when {
      ctx.isDeclared(name) ->
        Failure("Variable '$name' ya fue declarada", ErrorType.SEMANTIC)
      !ctx.isValidType(type) ->
        Failure("Tipo '$type' no reconocido", ErrorType.SEMANTIC)
      else -> {
        val exprType = ctx.resolveExpressionType(value)
        when (exprType) {
          is Failure -> Failure(exprType.msg, exprType.type)
          is Success -> {
            if (exprType.data != type) {
              Failure("Se esperaba '$type' pero se obtuvo '${exprType.data}'", ErrorType.SEMANTIC)
            } else {
              ctx.declare(name, type)
              Success("ok", Unit)
            }
          }
        }
      }
    }
  }
)

val AssignmentDef = StatementDef(
  tag = "Assignment",
  fields = mapOf(
    "target" to FieldType.TEXT,
    "value" to FieldType.EXPRESSION
  ),
  semanticCheck = { fields, ctx ->
    val target = fields.text("target")
    val value = fields.expression("value")

    val targetType = ctx.typeOf(target)
    if (targetType == null) {
      Failure("Variable '$target' no declarada", ErrorType.SEMANTIC)
    } else {
      val exprType = ctx.resolveExpressionType(value)
      when (exprType) {
        is Failure -> Failure(exprType.msg, exprType.type)
        is Success -> {
          if (exprType.data != targetType) {
            Failure("No se puede asignar '${exprType.data}' a '$target' de tipo '$targetType'", ErrorType.SEMANTIC)
          } else {
            Success("ok", Unit)
          }
        }
      }
    }
  }
)

val CallDef = StatementDef(
  tag = "Call",
  fields = mapOf(
    "function" to FieldType.TEXT,
    "arguments" to FieldType.EXPRESSIONS
  ),
  semanticCheck = { fields, ctx ->
    val args = fields.expressions("arguments")
    val errors = args.mapNotNull { arg ->
      val result = ctx.resolveExpressionType(arg)
      if (result is Failure) result.msg else null
    }
    if (errors.isNotEmpty()) {
      Failure(errors.first(), ErrorType.SEMANTIC)
    } else {
      Success("ok", Unit)
    }
  }
)

// =============================================================================
// GRAMMARS — con Steps etiquetados
// =============================================================================

val VariableDeclaration = Grammar(
  tag = "Declaration",
  steps = listOf(
    Step(IsStrat(CncKeywords.LET)),
    Step(IsStrat(CncPatterns.IDENTIFIER), label = "name"),
    Step(IsStrat(CncSymbols.COLON)),
    Step(AnyOfTypeStrat(CncKeywords.types), label = "type"),
    Step(IsStrat(CncSymbols.ASSIGN)),
    Step(ExpressionStrat(expressionTokens), label = "value"),
    Step(IsStrat(CncSymbols.SEMICOLON))
  ),
  statementDef = DeclarationDef,
  expressionBuilder = expressionBuilder
)

val VariableAssignment = Grammar(
  tag = "Assignment",
  steps = listOf(
    Step(IsStrat(CncPatterns.IDENTIFIER), label = "target"),
    Step(IsStrat(CncSymbols.ASSIGN)),
    Step(ExpressionStrat(expressionTokens), label = "value"),
    Step(IsStrat(CncSymbols.SEMICOLON))
  ),
  statementDef = AssignmentDef,
  expressionBuilder = expressionBuilder
)

val FunctionCall = Grammar(
  tag = "Call",
  steps = listOf(
    Step(IsStrat(CncPatterns.IDENTIFIER), label = "function"),
    Step(IsStrat(CncSymbols.OPEN_PAREN)),
    Step(ExpressionStrat(listOf(
      CncPatterns.NUMBER,
      CncPatterns.STRING,
      CncPatterns.IDENTIFIER,
      CncSymbols.PLUS,
      CncSymbols.MINUS,
      CncSymbols.MULTIPLICATION,
      CncSymbols.DIVISION
    )), label = "arguments"),
    Step(IsStrat(CncSymbols.CLOSE_PAREN)),
    Step(IsStrat(CncSymbols.SEMICOLON))
  ),
  statementDef = CallDef,
  expressionBuilder = expressionBuilder
)

// =============================================================================
// PARSER CONFIGURATION
// =============================================================================

val terminators: List<TokenDefinition> = listOf(
  CncSymbols.SEMICOLON
)

val grammars = listOf(
  VariableDeclaration,
  FunctionCall,
  VariableAssignment
)

// EXPRESSIONS BUILDER ==========================================================
val expressionBuilder = ExpressionBuilder(mapOf(
  CncPatterns.NUMBER to { token: Token -> NumberLiteral(token.text.toDouble()) },
  CncPatterns.STRING to { token: Token -> StringLiteral(token.text.removeSurrounding("\"")) },
  CncPatterns.IDENTIFIER to { token -> Identifier(token.text) }
))

// INTERPRETER CONFIGURATION ====================================================

val printScriptStatementEvaluators = mapOf(
  Declaration::class to DeclarationEvaluator(),
  Assignment::class to AssignmentEvaluator(),
  Call::class to CallEvaluator()
)



val printScriptBinaryOperations: Map<String, BinaryOperation> = mapOf(    // La clase Standard Binary Ops es general, pero el usuario puede crear lo q quiera
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

// =============================================================================
// SEMANTIC CONFIGURATION
// =============================================================================

val binaryTypeRules: Map<String, BinaryOpResolver> = mapOf(
  "+"  to TypeResolvers.additionOrConcat,
  "-"  to TypeResolvers.numericOnly("-"),
  "*"  to TypeResolvers.numericOnly("*"),
  "/"  to TypeResolvers.numericOnly("/"),
)

val symbolTable = SymbolTable(validTypes = setOf("number", "string"))

val semanticContext = DefaultSemanticContext(symbolTable, binaryTypeRules)
