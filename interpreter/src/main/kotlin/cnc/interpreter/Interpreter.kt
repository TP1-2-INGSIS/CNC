package cnc.interpreter

import cnc.ast.Expression
import cnc.ast.Fields
import cnc.ast.GenericStatement
import kotlin.reflect.KClass

class Environment {
    private val variables = mutableMapOf<String, Any?>() // Estado del programa del cliente, guardamos en string
                                                         // nombre de variable mapeando con el valor en su tipo concreto

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
// Data-driven: cada evaluador se mapea por `tag` (coherente con :semantic, que
// usa StatementDef.semanticCheck, y con :formatter, que mapea por tag). Recibe
// los Fields del GenericStatement en lugar de una subclase de Statement.
interface StatementEvaluator {
    fun evaluate(fields: Fields, environment: Environment, interpreter: Interpreter)
}

// Expressions
interface ExpressionEvaluator<T : Expression> {
    fun evaluate(expression: T, environment: Environment, interpreter: Interpreter): Any? // Devolvemos any porque pueden haber operaciones
}

class Interpreter(
    private val statementEvaluators: Map<String, StatementEvaluator>,
    private val expressionEvaluators: Map<KClass<out Expression>, ExpressionEvaluator<out Expression>>
) {

    fun interpret(statements: List<GenericStatement>, environment: Environment) {
        for (statement in statements) {
            interpret(statement, environment)
        }
    }

    fun interpret(statement: GenericStatement, environment: Environment) {
        val evaluator = statementEvaluators[statement.tag]
            ?: throw RuntimeException("No evaluator registered for statement tag: '${statement.tag}'")

        evaluator.evaluate(statement.fields, environment, this)
    }

    @Suppress("UNCHECKED_CAST")
    fun evaluate(expression: Expression, environment: Environment): Any? {
        val evaluator = expressionEvaluators[expression::class] as? ExpressionEvaluator<Expression>
            ?: throw RuntimeException("No evaluator registered for expression type: ${expression::class.simpleName}")

        return evaluator.evaluate(expression, environment, this)
    }
}
