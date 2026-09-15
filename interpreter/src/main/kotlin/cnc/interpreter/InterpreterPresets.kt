package cnc.interpreter

import cnc.ast.Assignment
import cnc.ast.Call
import cnc.ast.Declaration
import cnc.ast.Statement
import kotlin.reflect.KClass

class InterpreterBuilder {
    private val statementEvaluators = mutableMapOf<KClass<out Statement>, StatementEvaluator<*>>()
    private val binaryOperations = mutableMapOf<String, BinaryOperation>()
    private val unaryOperations = mutableMapOf<String, UnaryOperation>()
    private val builtins = mutableMapOf<String, BuiltinMethod>()
    private var output: ((String) -> Unit)? = null

    fun registerStatementEvaluator(
        kClass: KClass<out Statement>,
        evaluator: StatementEvaluator<*>
    ): InterpreterBuilder = apply {
        statementEvaluators[kClass] = evaluator
    }

    inline fun <reified T : Statement> registerStatement(evaluator: StatementEvaluator<T>): InterpreterBuilder =
        registerStatementEvaluator(T::class, evaluator)

    fun registerOperation(operator: String, operation: BinaryOperation): InterpreterBuilder = apply {
        binaryOperations[operator] = operation
    }

    fun registerUnaryOperation(operator: String, operation: UnaryOperation): InterpreterBuilder = apply {
        unaryOperations[operator] = operation
    }

    fun registerBuiltin(builtin: BuiltinMethod): InterpreterBuilder = apply {
        builtins[builtin.name] = builtin
    }

    fun withOutput(output: (String) -> Unit): InterpreterBuilder = apply {
        this.output = output
    }

    fun build(): Interpreter {
        val out = output ?: ::println
        if (!builtins.containsKey("println")) {
            builtins["println"] = BuiltinMethodFactory.println(out)
        }

        if (!statementEvaluators.containsKey(Declaration::class)) {
            statementEvaluators[Declaration::class] = DeclarationEvaluator()
        }
        if (!statementEvaluators.containsKey(Assignment::class)) {
            statementEvaluators[Assignment::class] = AssignmentEvaluator()
        }
        if (!statementEvaluators.containsKey(Call::class)) {
            statementEvaluators[Call::class] = CallEvaluator(builtins)
        }
        if (!statementEvaluators.containsKey(cnc.ast.BlockStatement::class)) {
            statementEvaluators[cnc.ast.BlockStatement::class] = BlockEvaluator()
        }
        if (!statementEvaluators.containsKey(cnc.ast.IfStatement::class)) {
            statementEvaluators[cnc.ast.IfStatement::class] = IfEvaluator()
        }

        if (!binaryOperations.containsKey("+")) {
            binaryOperations["+"] = BinaryOperation(StandardBinaryOperations::add)
        }
        if (!binaryOperations.containsKey("-")) {
            binaryOperations["-"] = BinaryOperation(StandardBinaryOperations::subtract)
        }
        if (!binaryOperations.containsKey("*")) {
            binaryOperations["*"] = BinaryOperation(StandardBinaryOperations::multiply)
        }
        if (!binaryOperations.containsKey("/")) {
            binaryOperations["/"] = BinaryOperation(StandardBinaryOperations::divide)
        }

        if (!unaryOperations.containsKey("-")) {
            unaryOperations["-"] = UnaryOperation(StandardUnaryOperations::negate)
        }
        if (!unaryOperations.containsKey("+")) {
            unaryOperations["+"] = UnaryOperation(StandardUnaryOperations::positive)
        }

        val expressionEvaluator = ExpressionEvaluator(
            binaryOperations = binaryOperations.toMap(),
            unaryOperations = unaryOperations.toMap(),
            builtins = builtins.toMap()
        )
        return Interpreter(statementEvaluators.toMap(), expressionEvaluator)
    }
}

object InterpreterPresets {

    fun builder(): InterpreterBuilder = InterpreterBuilder()

    fun v1_0(output: (String) -> Unit = ::println): Interpreter {
        return builder()
            .withOutput(output)
            .build()
    }

    fun v1_1(
        input: (String) -> String = { readln() },
        envProvider: (String) -> String? = System::getenv,
        output: (String) -> Unit = ::println
    ): Interpreter {
        return builder()
            .withOutput(output)
            .registerBuiltin(BuiltinMethodFactory.readInput(input))
            .registerBuiltin(BuiltinMethodFactory.readEnv(envProvider))
            .build()
    }
}
