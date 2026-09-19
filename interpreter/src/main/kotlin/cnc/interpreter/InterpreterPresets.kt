package cnc.interpreter

import cnc.ast.Assignment
import cnc.ast.BlockStatement
import cnc.ast.Call
import cnc.ast.Declaration
import cnc.ast.IfStatement
import cnc.ast.Statement
import kotlin.reflect.KClass

class InterpreterBuilder {
    private val builtins = mutableMapOf<String, BuiltinMethod>()
    private var output: ((String) -> Unit)? = null
    private var input: ((String) -> String)? = null
    private var envProvider: ((String) -> String?)? = null

    fun registerBuiltin(builtin: BuiltinMethod): InterpreterBuilder = apply {
        builtins[builtin.name] = builtin
    }

    fun withOutput(output: (String) -> Unit): InterpreterBuilder = apply {
        this.output = output
    }

    fun withInput(input: (String) -> String): InterpreterBuilder = apply {
        this.input = input
    }

    fun withEnvProvider(envProvider: (String) -> String?): InterpreterBuilder = apply {
        this.envProvider = envProvider
    }

    fun build(): Interpreter = InterpreterPresets.default(
        input = input ?: { readln() },
        envProvider = envProvider ?: System::getenv,
        output = output ?: ::println,
        customBuiltins = builtins.toMap()
    )
}

object InterpreterPresets {

    fun builder(): InterpreterBuilder = InterpreterBuilder()

    fun default(
        input: (String) -> String = { readln() },
        envProvider: (String) -> String? = System::getenv,
        output: (String) -> Unit = ::println,
        customBuiltins: Map<String, BuiltinMethod> = emptyMap()
    ): Interpreter {
        // 1. Built-ins declarados todos juntos de una sola vez
        val builtins: Map<String, BuiltinMethod> = mapOf(
            "println" to BuiltinMethodFactory.println(output),
            "readInput" to BuiltinMethodFactory.readInput(input),
            "readEnv" to BuiltinMethodFactory.readEnv(envProvider)
        ) + customBuiltins

        // 2. Evaluadores de sentencias construidos de una vez
        val statementEvaluators: Map<KClass<out Statement>, StatementEvaluator<*>> = mapOf(
            Declaration::class to DeclarationEvaluator(),
            Assignment::class to AssignmentEvaluator(),
            Call::class to CallEvaluator(builtins),
            BlockStatement::class to BlockEvaluator(),
            IfStatement::class to IfEvaluator()
        )

        // 3. Operaciones binarias construidas de una vez
        val binaryOperations: Map<String, BinaryOperation> = mapOf(
            "+" to BinaryOperation(StandardBinaryOperations::add),
            "-" to BinaryOperation(StandardBinaryOperations::subtract),
            "*" to BinaryOperation(StandardBinaryOperations::multiply),
            "/" to BinaryOperation(StandardBinaryOperations::divide)
        )

        // 4. Operaciones unarias construidas de una vez
        val unaryOperations: Map<String, UnaryOperation> = mapOf(
            "-" to UnaryOperation(StandardUnaryOperations::negate),
            "+" to UnaryOperation(StandardUnaryOperations::positive)
        )

        // 5. Evaluador de expresiones y ensamblado final
        val expressionEvaluator = ExpressionEvaluator(
            binaryOperations = binaryOperations,
            unaryOperations = unaryOperations,
            builtins = builtins
        )

        return Interpreter(statementEvaluators, expressionEvaluator)
    }

    fun create(
        input: (String) -> String = { readln() },
        envProvider: (String) -> String? = System::getenv,
        output: (String) -> Unit = ::println,
        customBuiltins: Map<String, BuiltinMethod> = emptyMap()
    ): Interpreter = default(input, envProvider, output, customBuiltins)
}
