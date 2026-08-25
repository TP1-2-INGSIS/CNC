package cnc.semantic

import ast.StatementVisitor
import cnc.ast.Assignment
import cnc.ast.Call
import cnc.ast.Declaration
import cnc.ast.Statement
import cnc.common.ErrorType
import cnc.common.Failure
import cnc.common.Result
import cnc.common.Success

data class Param(val name: String, val type: String)
data class MethodSignature(val params: List<Param>, val returnType: String)

class SemanticVisitor(
    private val binaryRules: Map<String, BinaryOpResolver>,
    private val symbolTable: SymbolTable
) : StatementVisitor<Result<Statement>> {

    private val declaredMethods = mutableMapOf(
        "println" to MethodSignature(listOf(Param("value", "any")), "void")
    )

    private val exprChecker get() = ExpressionTypeVisitor(symbolTable.asReadOnly(), binaryRules)

    override fun visit(s: Declaration): Result<Statement> {
        if (symbolTable.isDeclared(s.name))
            return Failure("Variable '${s.name}' ya fue declarada", ErrorType.SEMANTIC)

        if (!symbolTable.isValidType(s.type))
            return Failure("Tipo '${s.type}' no reconocido", ErrorType.SEMANTIC)

        val value = s.value
        if (value != null) {
            val result = value.accept(exprChecker)
            if (result is Failure) return Failure(result.msg, result.type)

            val valueType = (result as Success).data
            if (valueType != s.type)
                return Failure("Se esperaba '${s.type}' pero se obtuvo '$valueType'", ErrorType.SEMANTIC)
        }

        symbolTable.declare(s.name, s.type)
        return Success("ok", s)
    }

    override fun visit(s: Assignment): Result<Statement> {
        val targetType = symbolTable.typeOf(s.target)
            ?: return Failure("Variable '${s.target}' no declarada", ErrorType.SEMANTIC)

        val result = s.value.accept(exprChecker)
        if (result is Failure) return Failure(result.msg, result.type)

        val valueType = (result as Success).data
        if (valueType != targetType)
            return Failure("No se puede asignar '$valueType' a '${s.target}' de tipo '$targetType'", ErrorType.SEMANTIC)

        return Success("ok", s)
    }

    override fun visit(s: Call): Result<Statement> {
        val signature = declaredMethods[s.function]
            ?: return Failure("Función '${s.function}' no declarada", ErrorType.SEMANTIC)

        if (s.arguments.size != signature.params.size)
            return Failure(
                "'${s.function}' espera ${signature.params.size} args, recibió ${s.arguments.size}",
                ErrorType.SEMANTIC
            )

        for ((arg, param) in s.arguments.zip(signature.params)) {
            val result = arg.accept(exprChecker)
            if (result is Failure) return Failure(result.msg, result.type)

            val argType = (result as Success).data
            if (param.type != "any" && argType != param.type)
                return Failure(
                    "Argumento '${param.name}': esperaba '${param.type}', recibió '$argType'",
                    ErrorType.SEMANTIC
                )
        }

        return Success("ok", s)
    }
}