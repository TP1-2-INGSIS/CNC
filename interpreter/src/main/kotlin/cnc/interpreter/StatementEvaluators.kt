package cnc.interpreter

import cnc.ast.Assignment
import cnc.ast.Call
import cnc.ast.Declaration
import cnc.common.ErrorType
import cnc.common.Failure
import cnc.common.Result
import cnc.common.Success

class DeclarationEvaluator : StatementEvaluator<Declaration> {
    override fun evaluate(statement: Declaration, environment: Environment, interpreter: Interpreter): Result<Unit> {
        val valueExpr = statement.value
        val (initialValue, isInitialized) = if (valueExpr != null) {
            val evalResult = interpreter.evaluate(valueExpr, environment)
            if (evalResult is Failure) return Failure(evalResult.msg, evalResult.type)
            Pair((evalResult as Success).data, true)
        } else {
            Pair(null, false)
        }

        return environment.define(
            name = statement.name,
            value = initialValue,
            isMutable = statement.isMutable,
            isInitialized = isInitialized
        )
    }
}

class AssignmentEvaluator : StatementEvaluator<Assignment> {
    override fun evaluate(statement: Assignment, environment: Environment, interpreter: Interpreter): Result<Unit> {
        val evalResult = interpreter.evaluate(statement.value, environment)
        if (evalResult is Failure) return Failure(evalResult.msg, evalResult.type)
        val value = (evalResult as Success).data

        return environment.assign(statement.target, value)
    }
}

class CallEvaluator(
    private val builtins: Map<String, BuiltinMethod>
) : StatementEvaluator<Call> {

    constructor(output: (String) -> Unit = ::println) : this(
        mapOf("println" to BuiltinMethodFactory.println(output))
    )

    override fun evaluate(statement: Call, environment: Environment, interpreter: Interpreter): Result<Unit> {
        val builtin = builtins[statement.function]
            ?: return Failure("Unknown function: '${statement.function}'", ErrorType.RUNTIME)

        val evaluatedArgs = mutableListOf<Any?>()
        for (arg in statement.arguments) {
            val evalResult = interpreter.evaluate(arg, environment)
            if (evalResult is Failure) return Failure(evalResult.msg, evalResult.type)
            evaluatedArgs.add((evalResult as Success).data)
        }

        val execResult = builtin.execute(evaluatedArgs)
        if (execResult is Failure) return Failure(execResult.msg, execResult.type)
        return Success("ok", Unit)
    }
}

class BlockEvaluator : StatementEvaluator<cnc.ast.BlockStatement> {
    override fun evaluate(statement: cnc.ast.BlockStatement, environment: Environment, interpreter: Interpreter): Result<Unit> {
        val blockEnv = environment.createChild()
        for (stmt in statement.statements) {
            val res = interpreter.interpret(stmt, blockEnv)
            if (res is Failure) return res
        }
        return Success("ok", Unit)
    }
}

class IfEvaluator : StatementEvaluator<cnc.ast.IfStatement> {
    override fun evaluate(statement: cnc.ast.IfStatement, environment: Environment, interpreter: Interpreter): Result<Unit> {
        val condRes = interpreter.evaluate(statement.condition, environment)
        if (condRes is Failure) return Failure(condRes.msg, condRes.type)
        
        val conditionValue = (condRes as Success).data
        if (conditionValue !is Boolean) {
            return Failure("Condition must evaluate to boolean, got ${conditionValue?.let { it::class.simpleName }}", ErrorType.RUNTIME)
        }

        if (conditionValue) {
            return interpreter.interpret(statement.thenBlock, environment) // BlockEvaluator will create its own child scope
        } else {
            val elseBlock = statement.elseBlock
            if (elseBlock != null) {
                return interpreter.interpret(elseBlock, environment)
            }
        }
        
        return Success("ok", Unit)
    }
}
