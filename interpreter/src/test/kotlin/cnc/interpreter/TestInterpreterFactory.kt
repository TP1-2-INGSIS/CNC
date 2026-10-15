package cnc.interpreter

import cnc.ast.Assignment
import cnc.ast.BlockStatement
import cnc.ast.Call
import cnc.ast.Declaration
import cnc.ast.IfStatement
import cnc.common.BuiltinMethod

internal object TestInterpreterFactory {
    fun create(
        output: (String) -> Unit = {},
        input: (String) -> String = { "" },
        envProvider: (String) -> String? = { null },
        customBuiltins: Map<String, BuiltinMethod> = emptyMap(),
    ): Interpreter {
        val builtins =
            mapOf(
                "println" to BuiltinMethodFactory.println(output),
                "readInput" to BuiltinMethodFactory.readInput(input),
                "readEnv" to BuiltinMethodFactory.readEnv(envProvider),
            ) + customBuiltins

        return Interpreter
            .builder()
            .registerEvaluators(
                mapOf(
                    Declaration::class to DeclarationEvaluator(),
                    Assignment::class to AssignmentEvaluator(),
                    Call::class to CallEvaluator(builtins),
                    BlockStatement::class to BlockEvaluator(),
                    IfStatement::class to IfEvaluator(),
                ),
            ).registerBinaryOperations(
                mapOf(
                    "+" to BinaryOperation(StandardBinaryOperations::add),
                    "-" to BinaryOperation(StandardBinaryOperations::subtract),
                    "*" to BinaryOperation(StandardBinaryOperations::multiply),
                    "/" to BinaryOperation(StandardBinaryOperations::divide),
                ),
            ).registerUnaryOperations(
                mapOf(
                    "-" to UnaryOperation(StandardUnaryOperations::negate),
                    "+" to UnaryOperation(StandardUnaryOperations::positive),
                ),
            ).registerBuiltins(builtins)
            .build()
    }
}
