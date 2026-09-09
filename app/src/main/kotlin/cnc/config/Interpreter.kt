package cnc.config

import cnc.ast.BinaryExpression
import cnc.ast.Expression
import cnc.ast.Identifier
import cnc.ast.NumberLiteral
import cnc.ast.StringLiteral

import cnc.interpreter.AssignmentEvaluator
import cnc.interpreter.BinaryExpressionEvaluator
import cnc.interpreter.BinaryOperation
import cnc.interpreter.DeclarationEvaluator
import cnc.interpreter.ExpressionEvaluator
import cnc.interpreter.IdentifierEvaluator
import cnc.interpreter.Interpreter
import cnc.interpreter.NumberLiteralEvaluator
import cnc.interpreter.StandardBinaryOperations
import cnc.interpreter.StatementEvaluator
import cnc.interpreter.StringLiteralEvaluator

import kotlin.reflect.KClass

/** Operadores binarios soportados en ejecución. */
private val binaryOperations: Map<String, BinaryOperation> = mapOf(
  "+" to BinaryOperation { l, r -> StandardBinaryOperations.add(l, r) },
  "-" to BinaryOperation { l, r -> StandardBinaryOperations.subtract(l, r) },
  "*" to BinaryOperation { l, r -> StandardBinaryOperations.multiply(l, r) },
  "/" to BinaryOperation { l, r -> StandardBinaryOperations.divide(l, r) }
)

/** Evaluadores de statements, mapeados por `tag` (data-driven). */
private val statementEvaluators: Map<String, StatementEvaluator> = mapOf(
  "VariableDeclaration" to DeclarationEvaluator(),
  "VariableAssignment" to AssignmentEvaluator()
)

/** Evaluadores de expresiones, mapeados por tipo de nodo. */
private val expressionEvaluators: Map<KClass<out Expression>, ExpressionEvaluator<out Expression>> = mapOf(
  NumberLiteral::class to NumberLiteralEvaluator(),
  StringLiteral::class to StringLiteralEvaluator(),
  Identifier::class to IdentifierEvaluator(),
  BinaryExpression::class to BinaryExpressionEvaluator(binaryOperations)
)

/** Intérprete de PrintScript, listo para inyectar/usar. */
val printScriptInterpreter = Interpreter(statementEvaluators, expressionEvaluators)
