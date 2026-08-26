package cnc.interpreter

import cnc.ast.Expression
import cnc.ast.Statement
import kotlin.reflect.KClass

class Environment {
    private val variables = mutableMapOf<String, Any?>()

    fun define(name: String, value: Any?) {
        variables[name] = value
    }

    fun get(name: String): Any? {
        if (variables.containsKey(name)) {
            return variables[name]
        }
        throw RuntimeException("Undefined variable '$name'.")
    }

    fun assign(name: String, value: Any?) {
        if (variables.containsKey(name)) {
            variables[name] = value
            return
        }
        throw RuntimeException("Undefined variable '$name'.")
    }
}

// STATEMENTS
interface StatementEvaluator<T : Statement> {
    fun evaluate(statement: T, environment: Environment, interpreter: Interpreter)
}

// Expressions
interface ExpressionEvaluator<T : Expression> {
    fun evaluate(expression: T, environment: Environment, interpreter: Interpreter): Any?
}

class Interpreter(
    private val statementEvaluators: Map<KClass<out Statement>, StatementEvaluator<out Statement>>,
    private val expressionEvaluators: Map<KClass<out Expression>, ExpressionEvaluator<out Expression>>
) {

    fun interpret(statements: List<Statement>, environment: Environment) {
        for (statement in statements) {
            interpret(statement, environment)
        }
    }

    @Suppress("UNCHECKED_CAST")
    fun interpret(statement: Statement, environment: Environment) {
        val evaluator = statementEvaluators[statement::class] as? StatementEvaluator<Statement>
            ?: throw RuntimeException("No evaluator registered for statement type: ${statement::class.simpleName}")
        
        evaluator.evaluate(statement, environment, this)
    }

    @Suppress("UNCHECKED_CAST")
    fun evaluate(expression: Expression, environment: Environment): Any? {
        val evaluator = expressionEvaluators[expression::class] as? ExpressionEvaluator<Expression>
            ?: throw RuntimeException("No evaluator registered for expression type: ${expression::class.simpleName}")
        
        return evaluator.evaluate(expression, environment, this)
    }
}
