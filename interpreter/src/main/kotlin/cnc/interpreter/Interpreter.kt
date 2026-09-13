package cnc.interpreter

import cnc.ast.Expression
import cnc.ast.Statement
import cnc.common.ErrorType
import cnc.common.Failure
import cnc.common.Result
import cnc.common.Success
import kotlin.reflect.KClass

class Environment(
    private val parent: Environment? = null
) {
    private val variables = mutableMapOf<String, Any?>()
    private val mutability = mutableMapOf<String, Boolean>()
    private val initialized = mutableSetOf<String>()

    fun define(
        name: String,
        value: Any? = null,
        isMutable: Boolean = true,
        isInitialized: Boolean = (value != null)
    ): Result<Unit> {
        if (variables.containsKey(name)) {
            return Failure("Variable '$name' is already defined in this scope", ErrorType.RUNTIME)
        }
        variables[name] = value
        mutability[name] = isMutable
        if (isInitialized) {
            initialized.add(name)
        }
        return Success("ok", Unit)
    }

    fun get(name: String): Result<Any?> {
        if (variables.containsKey(name)) {
            if (!initialized.contains(name)) {
                return Failure("Variable '$name' has not been initialized", ErrorType.RUNTIME)
            }
            return Success("ok", variables[name])
        }
        if (parent != null) {
            return parent.get(name)
        }
        return Failure("Undefined variable '$name'", ErrorType.RUNTIME)
    }

    fun assign(name: String, value: Any?): Result<Unit> {
        if (variables.containsKey(name)) {
            if (mutability[name] == false) {
                return Failure("Cannot reassign to immutable variable '$name'", ErrorType.RUNTIME)
            }
            variables[name] = value
            initialized.add(name)
            return Success("ok", Unit)
        }
        if (parent != null) {
            return parent.assign(name, value)
        }
        return Failure("Undefined variable '$name'", ErrorType.RUNTIME)
    }

    fun isDeclared(name: String): Boolean {
        return variables.containsKey(name) || parent?.isDeclared(name) == true
    }

    fun createChild(): Environment = Environment(this)
}

// STATEMENTS
interface StatementEvaluator<T : Statement> {
    fun evaluate(statement: T, environment: Environment, interpreter: Interpreter): Result<Unit>
}

class Interpreter(
    private val statementEvaluators: Map<KClass<out Statement>, StatementEvaluator<*>>,
    private val expressionEvaluator: ExpressionEvaluator
) {

    fun interpret(statements: Iterable<Statement>, environment: Environment): Result<Unit> {
        for (statement in statements) {
            val result = interpret(statement, environment)
            if (result is Failure) return result
        }
        return Success("ok", Unit)
    }

    fun interpretAll(statements: Iterable<Statement>, environment: Environment): Sequence<Result<Unit>> = sequence {
        for (statement in statements) {
            val result = interpret(statement, environment)
            yield(result)
            if (result is Failure) break
        }
    }

    @Suppress("UNCHECKED_CAST")
    fun interpret(statement: Statement, environment: Environment): Result<Unit> {
        val evaluator = statementEvaluators[statement::class] as? StatementEvaluator<Statement>
            ?: return Failure("No evaluator registered for statement type: ${statement::class.simpleName}", ErrorType.RUNTIME)

        return evaluator.evaluate(statement, environment, this)
    }

    fun evaluate(expression: Expression, environment: Environment): Result<Any?> {
        return expressionEvaluator.evaluate(expression, environment, this)
    }
}
