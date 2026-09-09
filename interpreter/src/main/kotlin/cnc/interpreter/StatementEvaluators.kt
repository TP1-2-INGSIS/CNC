package cnc.interpreter

import cnc.ast.Fields

/**
 * Evalúa una declaración de variable: `let name: type = value;`.
 * Campos esperados (definidos por la Grammar): `name` (TEXT), `value` (EXPRESSION).
 */
class DeclarationEvaluator : StatementEvaluator {
    override fun evaluate(fields: Fields, environment: Environment, interpreter: Interpreter) {
        val initialValue = if (fields.has("value")) {
            interpreter.evaluate(fields.expression("value"), environment)
        } else {
            null
        }
        environment.define(fields.text("name"), initialValue)
    }
}

/**
 * Evalúa una asignación: `target = value;`.
 * Campos esperados: `target` (TEXT), `value` (EXPRESSION).
 */
class AssignmentEvaluator : StatementEvaluator {
    override fun evaluate(fields: Fields, environment: Environment, interpreter: Interpreter) {
        val value = interpreter.evaluate(fields.expression("value"), environment)
        environment.assign(fields.text("target"), value)
    }
}

/**
 * Evalúa una llamada a función soportada (p. ej. `println(args);`).
 * Campos esperados: `function` (TEXT), `arguments` (EXPRESSIONS).
 */
class CallEvaluator(
    private val output: (String) -> Unit = { println(it) }
) : StatementEvaluator {
    override fun evaluate(fields: Fields, environment: Environment, interpreter: Interpreter) {
        val evaluatedArgs = fields.expressions("arguments").map { interpreter.evaluate(it, environment) }

        when (val function = fields.text("function")) {
            "println" -> output(evaluatedArgs.joinToString(" ") { formatOutput(it) })
            else -> throw RuntimeException("Unknown function: '$function'")
        }
    }

    private fun formatOutput(value: Any?): String {
        if (value is Double && value % 1.0 == 0.0) {
            return value.toInt().toString()
        }
        return value?.toString() ?: "null"
    }
}
