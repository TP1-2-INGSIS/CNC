package cnc.linter

import cnc.ast.Assignment
import cnc.ast.BinaryExpression
import cnc.ast.BlockStatement
import cnc.ast.BooleanLiteral
import cnc.ast.Call
import cnc.ast.CallExpression
import cnc.ast.Declaration
import cnc.ast.Expression
import cnc.ast.Identifier
import cnc.ast.IfStatement
import cnc.ast.NumberLiteral
import cnc.ast.Statement
import cnc.ast.StringLiteral
import cnc.ast.UnaryExpression

class SimpleFunctionCallRule(
    val functionName: String
) : LinterRule {

    override fun check(statement: Statement): List<String> {
        val allCallsArguments = mutableListOf<List<Expression>>()

        // 1. Sentencia directa: ej. println(x); o readInput(x);
        if (statement is Call && statement.function == functionName) {
            allCallsArguments.add(statement.arguments)
        }

        // 2. Llamadas anidadas adentro de las expresiones del statement: ej. let x = readInput(...)
        for (expr in statement.expressions()) {
            for (call in expr.findCalls(functionName)) {
                allCallsArguments.add(call.arguments)
            }
        }

        // 3. Validar que cada argumento sea únicamente variable o literal
        val warnings = mutableListOf<String>()
        for (arguments in allCallsArguments) {
            for (arg in arguments) {
                if (!isVariableOrLiteral(arg)) {
                    warnings.add("La llamada a '$functionName' solo permite una variable o un literal como argumento.")
                }
            }
        }
        return warnings
    }

    private fun isVariableOrLiteral(expr: Expression): Boolean =
        expr is Identifier || expr is NumberLiteral || expr is StringLiteral || expr is BooleanLiteral

    private fun Statement.expressions(): List<Expression> = when (this) {
        is Declaration -> listOfNotNull(value)
        is Assignment -> listOf(value)
        is Call -> arguments
        is IfStatement -> listOf(condition)
        is BlockStatement -> emptyList()
    }

    private fun Expression.findCalls(targetFunction: String): List<CallExpression> = when (this) {
        is CallExpression -> {
            val matches = if (function == targetFunction) listOf(this) else emptyList()
            matches + arguments.flatMap { it.findCalls(targetFunction) }
        }
        is BinaryExpression -> left.findCalls(targetFunction) + right.findCalls(targetFunction)
        is UnaryExpression -> operand.findCalls(targetFunction)
        else -> emptyList()
    }
}
