package cnc.semantic

import ast.StatementVisitor
import cnc.ast.Assignment
import cnc.ast.BinaryExpression
import cnc.ast.Call
import cnc.ast.Declaration
import cnc.ast.Expression
import cnc.ast.Identifier
import cnc.ast.NumberLiteral
import cnc.ast.Statement
import cnc.ast.StringLiteral
import cnc.common.ErrorType
import cnc.common.Failure
import cnc.common.Result
import cnc.common.Success

data class Param(
    val name: String,
    val type: String
)

data class MethodSignature(
    val params: List<Param>,
    val returnType: String
)

class SemanticVisitor : StatementVisitor<Result<Statement>> {
    private val declaredVars = mutableMapOf<String, String>()
    private val declaredMethods = mutableMapOf(
        "println" to MethodSignature(
            params = listOf(Param("value", "any")),
            returnType = "void"
        )
    )

    override fun visit(s: Declaration): Result<Statement> {
        if (declaredVars.containsKey(s.name))
            return Failure("Variable '${s.name}' ya fue declarada", ErrorType.SEMANTIC)
        declaredVars[s.name] = s.type
        return Success("ok", s)
    }

    override fun visit(s: Assignment): Result<Statement> {
        if (!declaredVars.containsKey(s.target))
            return Failure("Variable '${s.target}' no fue declarada", ErrorType.SEMANTIC)
        return Success("ok", s)
    }

    override fun visit(s: Call): Result<Statement> {
        val signature = declaredMethods[s.function]
            ?: return Failure("Funcion '${s.function}' no declarada", ErrorType.SEMANTIC)

        if (s.arguments.size != signature.params.size)
            return Failure(
                "'${s.function}' espera ${signature.params.size} argumentos, recibio ${s.arguments.size}",
                ErrorType.SEMANTIC
            )

        s.arguments.zip(signature.params).forEach { (arg, param) ->
            val argType = inferType(arg)
            if (param.type != "any" && argType != param.type)
                return Failure(
                    "Argumento '${param.name}' esperaba ${param.type}, recibió $argType",
                    ErrorType.SEMANTIC
                )
        }

        return Success("ok", s)
    }

    private fun inferType(expr: Expression): String = when (expr) {
        is NumberLiteral    -> "number"
        is StringLiteral    -> "string"
        is Identifier       -> declaredVars[expr.name]
                                ?: error("Variable '${expr.name}' no declarada")
        is BinaryExpression -> inferType(expr.left)
    }
}