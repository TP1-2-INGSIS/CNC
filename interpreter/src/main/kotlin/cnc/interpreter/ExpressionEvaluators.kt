package cnc.interpreter

import cnc.ast.BinaryExpression
import cnc.ast.Expression
import cnc.ast.Identifier
import cnc.ast.NumberLiteral
import cnc.ast.StringLiteral
import kotlin.reflect.KClass

class NumberLiteralEvaluator : ExpressionEvaluator<NumberLiteral> {
    override fun evaluate(expression: NumberLiteral, environment: Environment, interpreter: Interpreter): Any {
        return expression.value
    }
}

class StringLiteralEvaluator : ExpressionEvaluator<StringLiteral> {
    override fun evaluate(expression: StringLiteral, environment: Environment, interpreter: Interpreter): Any {
        return expression.value
    }
}

class IdentifierEvaluator : ExpressionEvaluator<Identifier> {
    override fun evaluate(expression: Identifier, environment: Environment, interpreter: Interpreter): Any? {
        return environment.get(expression.name)
    }
}

class BinaryExpressionEvaluator(
    private val operators: Map<String, BinaryOperation>
) : ExpressionEvaluator<BinaryExpression> {
    override fun evaluate(expression: BinaryExpression, environment: Environment, interpreter: Interpreter): Any? {
        val left = interpreter.evaluate(expression.left, environment) ?: throw RuntimeException("Null operand")
        val right = interpreter.evaluate(expression.right, environment) ?: throw RuntimeException("Null operand")

        val operation = operators[expression.operator]
            ?: throw RuntimeException("Unsupported operator: '${expression.operator}'")

        return operation.execute(left, right)
    }
}
