package cnc.config

import cnc.ast.FieldType
import cnc.ast.StatementDef

import cnc.common.Result
import cnc.common.Success

import cnc.parser.Grammar
import cnc.parser.Step
import cnc.parser.ExpressionStrat
import cnc.parser.IsStrat
import cnc.parser.AnyOfTypeStrat

import cnc.token.TokenDefinition

// =============================================================================
// StatementDefs — definición declarativa de cada statement (tag + fields +
// validación semántica). La sintaxis concreta vive en la Grammar (steps).
// =============================================================================

private val declarationSemanticCheck: (cnc.ast.Fields, cnc.ast.SemanticContext) -> Result<Unit> =
  { fields, ctx ->
    val type = fields.text("type")
    val name = fields.text("name")
    when {
      !ctx.isValidType(type) ->
        cnc.common.Failure<Unit>("Tipo inválido: '$type'", cnc.common.ErrorType.SEMANTIC)
      ctx.isDeclared(name) ->
        cnc.common.Failure<Unit>("Variable '$name' ya declarada", cnc.common.ErrorType.SEMANTIC)
      else -> when (val resolved = ctx.resolveExpressionType(fields.expression("value"))) {
        is Success -> {
          ctx.declare(name, type)
          Success("ok", Unit)
        }
        is cnc.common.Failure -> cnc.common.Failure<Unit>(resolved.msg, resolved.type)
      }
    }
  }

private val assignmentSemanticCheck: (cnc.ast.Fields, cnc.ast.SemanticContext) -> Result<Unit> =
  { fields, ctx ->
    val target = fields.text("target")
    if (!ctx.isDeclared(target)) {
      cnc.common.Failure<Unit>("Variable '$target' no declarada", cnc.common.ErrorType.SEMANTIC)
    } else {
      when (val resolved = ctx.resolveExpressionType(fields.expression("value"))) {
        is Success -> Success("ok", Unit)
        is cnc.common.Failure -> cnc.common.Failure<Unit>(resolved.msg, resolved.type)
      }
    }
  }

val VariableDeclarationDef = StatementDef(
  tag = "VariableDeclaration",
  fields = mapOf(
    "name" to FieldType.TEXT,
    "type" to FieldType.TEXT,
    "value" to FieldType.EXPRESSION
  ),
  semanticCheck = declarationSemanticCheck
)

val VariableAssignmentDef = StatementDef(
  tag = "VariableAssignment",
  fields = mapOf(
    "target" to FieldType.TEXT,
    "value" to FieldType.EXPRESSION
  ),
  semanticCheck = assignmentSemanticCheck
)

// =============================================================================
// Gramáticas — secuencia de Steps etiquetados. Los labels deben coincidir con
// los nombres de fields del StatementDef.
// =============================================================================

private val expressionTokens: List<TokenDefinition> = listOf(
  CncPatterns.NUMBER,
  CncPatterns.STRING,
  CncPatterns.IDENTIFIER,
  CncSymbols.PLUS,
  CncSymbols.MINUS,
  CncSymbols.MULTIPLICATION,
  CncSymbols.DIVISION,
  CncSymbols.EXPONENT
)

val VariableDeclaration = Grammar(
  tag = "VariableDeclaration",
  steps = listOf(
    Step(IsStrat(CncKeywords.LET)),
    Step(IsStrat(CncPatterns.IDENTIFIER), label = "name"),
    Step(IsStrat(CncSymbols.COLON)),
    Step(AnyOfTypeStrat(CncKeywords.types), label = "type"),
    Step(IsStrat(CncSymbols.ASSIGN)),
    Step(ExpressionStrat(expressionTokens), label = "value"),
    Step(IsStrat(CncSymbols.SEMICOLON))
  ),
  statementDef = VariableDeclarationDef,
  expressionBuilder = expressionBuilder
)

val VariableAssignment = Grammar(
  tag = "VariableAssignment",
  steps = listOf(
    Step(IsStrat(CncPatterns.IDENTIFIER), label = "target"),
    Step(IsStrat(CncSymbols.ASSIGN)),
    Step(ExpressionStrat(expressionTokens), label = "value"),
    Step(IsStrat(CncSymbols.SEMICOLON))
  ),
  statementDef = VariableAssignmentDef,
  expressionBuilder = expressionBuilder
)

val terminators: List<TokenDefinition> = listOf(
  CncSymbols.SEMICOLON
)

val grammars = listOf(
  VariableDeclaration,
  VariableAssignment
)
