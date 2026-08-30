package cnc.interpreter

import cnc.ast.Assignment
import cnc.ast.Call
import cnc.ast.Declaration
import cnc.ast.Statement
import kotlin.reflect.KClass

class DeclarationEvaluator : StatementEvaluator<Declaration> {
    override fun evaluate(statement: Declaration, environment: Environment, interpreter: Interpreter) {
        val initialValue = statement.value?.let { interpreter.evaluate(it, environment) }
        environment.define(statement.name, initialValue)
    }
}

class AssignmentEvaluator : StatementEvaluator<Assignment> {
    override fun evaluate(statement: Assignment, environment: Environment, interpreter: Interpreter) {
        val value = interpreter.evaluate(statement.value, environment)
        environment.assign(statement.target, value)
    }
}

class CallEvaluator(
    private val output: (String) -> Unit = { println(it) }
) : StatementEvaluator<Call> {
    override fun evaluate(statement: Call, environment: Environment, interpreter: Interpreter) {
        val evaluatedArgs = statement.arguments.map { interpreter.evaluate(it, environment) }

        when (statement.function) {
            "println" -> output(evaluatedArgs.joinToString(" ") { formatOutput(it) })
            else -> throw RuntimeException("Unknown function: '${statement.function}'")
        }
    }

    private fun formatOutput(value: Any?): String {
        if (value is Double && value % 1.0 == 0.0) {
            return value.toInt().toString()
        }
        return value?.toString() ?: "null"
    }
}
