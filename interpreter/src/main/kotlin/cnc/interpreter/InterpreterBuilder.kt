package cnc.interpreter

import cnc.ast.Statement
import kotlin.reflect.KClass

class InterpreterBuilder {
    private val statementEvaluators = mutableMapOf<KClass<out Statement>, StatementEvaluator<*>>()
    private val binaryOperations = mutableMapOf<String, BinaryOperation>()
    private val unaryOperations = mutableMapOf<String, UnaryOperation>()
    private val builtins = mutableMapOf<String, BuiltinMethod>()

    fun registerEvaluator(kClass: KClass<out Statement>, evaluator: StatementEvaluator<*>): InterpreterBuilder = apply {
        statementEvaluators[kClass] = evaluator
    }

    fun registerEvaluators(evaluators: Map<KClass<out Statement>, StatementEvaluator<*>>): InterpreterBuilder = apply {
        statementEvaluators.putAll(evaluators)
    }

    fun registerBinaryOperation(op: String, operation: BinaryOperation): InterpreterBuilder = apply {
        binaryOperations[op] = operation
    }

    fun registerBinaryOperations(ops: Map<String, BinaryOperation>): InterpreterBuilder = apply {
        binaryOperations.putAll(ops)
    }

    fun registerUnaryOperation(op: String, operation: UnaryOperation): InterpreterBuilder = apply {
        unaryOperations[op] = operation
    }

    fun registerUnaryOperations(ops: Map<String, UnaryOperation>): InterpreterBuilder = apply {
        unaryOperations.putAll(ops)
    }

    fun registerBuiltin(builtin: BuiltinMethod): InterpreterBuilder = apply {
        builtins[builtin.name] = builtin
    }

    fun registerBuiltins(builtinsMap: Map<String, BuiltinMethod>): InterpreterBuilder = apply {
        builtins.putAll(builtinsMap)
    }

    fun copy(): InterpreterBuilder {
        val copy = InterpreterBuilder()
        copy.statementEvaluators.putAll(this.statementEvaluators)
        copy.binaryOperations.putAll(this.binaryOperations)
        copy.unaryOperations.putAll(this.unaryOperations)
        copy.builtins.putAll(this.builtins)
        return copy
    }

    fun build(): Interpreter {
        val exprEvaluator = ExpressionEvaluator(
            binaryOperations = binaryOperations.toMap(),
            unaryOperations = unaryOperations.toMap(),
            builtins = builtins.toMap()
        )
        return Interpreter(statementEvaluators.toMap(), exprEvaluator)
    }
}
