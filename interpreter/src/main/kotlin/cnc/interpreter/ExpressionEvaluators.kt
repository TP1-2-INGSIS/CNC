package cnc.interpreter

import cnc.ast.BinaryExpression
import cnc.ast.Expression
import cnc.ast.Identifier
import cnc.ast.NumberLiteral
import cnc.ast.StringLiteral
import cnc.ast.UnaryExpression
import cnc.common.ErrorType
import cnc.common.Failure
import cnc.common.Result
import cnc.common.Success
import cnc.common.map

import cnc.ast.CallExpression

class ExpressionEvaluator(
    private val binaryOperations: Map<String, BinaryOperation>,
    private val unaryOperations: Map<String, UnaryOperation> = emptyMap(),
    private val builtins: Map<String, BuiltinMethod> = emptyMap()
) {
    fun evaluate(expression: Expression, environment: Environment, interpreter: Interpreter): Result<Any?> {
        return when (expression) {
            is NumberLiteral -> Success("ok", ValueFormatter.formatNumber(expression.value))
            is StringLiteral -> Success("ok", expression.value)
            is cnc.ast.BooleanLiteral -> Success("ok", expression.value)
            is Identifier -> environment.get(expression.name)
            is CallExpression -> {
                val builtin = builtins[expression.function]
                    ?: return Failure("Function '${expression.function}' not found", ErrorType.RUNTIME)
                val args = mutableListOf<Any?>()
                for (arg in expression.arguments) {
                    val argRes = interpreter.evaluate(arg, environment)
                    if (argRes is Failure) return Failure(argRes.msg, argRes.type)
                    args.add((argRes as Success).data)
                }
                builtin.execute(args)
            }
            is BinaryExpression -> {
                val leftResult = interpreter.evaluate(expression.left, environment)
                if (leftResult is Failure) return Failure(leftResult.msg, leftResult.type)
                val left = (leftResult as Success).data
                    ?: return Failure("Null operand in binary expression", ErrorType.RUNTIME)

                val rightResult = interpreter.evaluate(expression.right, environment)
                if (rightResult is Failure) return Failure(rightResult.msg, rightResult.type)
                val right = (rightResult as Success).data
                    ?: return Failure("Null operand in binary expression", ErrorType.RUNTIME)

                val operation = binaryOperations[expression.operator]
                    ?: return Failure("Unsupported operator '${expression.operator}'", ErrorType.RUNTIME)

                operation.execute(left, right).map { it as Any? }
            }
            is UnaryExpression -> {
                val operandResult = interpreter.evaluate(expression.operand, environment)
                if (operandResult is Failure) return Failure(operandResult.msg, operandResult.type)
                val operand = (operandResult as Success).data
                    ?: return Failure("Null operand in unary expression", ErrorType.RUNTIME)

                val operation = unaryOperations[expression.operator]
                    ?: return Failure("Unsupported unary operator '${expression.operator}'", ErrorType.RUNTIME)

                operation.execute(operand).map { it as Any? }
            }
            else -> Failure("Unsupported expression type: ${expression::class.simpleName}", ErrorType.RUNTIME)
        }
    }
}
