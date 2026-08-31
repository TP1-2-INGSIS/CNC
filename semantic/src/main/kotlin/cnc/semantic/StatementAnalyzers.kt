package cnc.semantic

import cnc.ast.Assignment
import cnc.ast.Call
import cnc.ast.Declaration
import cnc.common.ErrorType
import cnc.common.Failure
import cnc.common.Result
import cnc.common.Success

class DeclarationAnalyzer : StatementAnalyzer<Declaration> {
    override fun analyze(statement: Declaration, context: SemanticContext, analyzer: SemanticAnalyzer): Result<cnc.ast.Statement> {
        if (context.symbolTable.isDeclared(statement.name))
            return Failure("Variable '${statement.name}' ya fue declarada", ErrorType.SEMANTIC)

        if (!context.symbolTable.isValidType(statement.type))
            return Failure("Tipo '${statement.type}' no reconocido", ErrorType.SEMANTIC)

        val value = statement.value
        if (value != null) {
            val result = analyzer.analyzeExpression(value, context)
            if (result is Failure) return Failure(result.msg, result.type)

            val valueType = (result as Success).data
            if (valueType != statement.type)
                return Failure("Se esperaba '${statement.type}' pero se obtuvo '$valueType'", ErrorType.SEMANTIC)
        }

        context.symbolTable.declare(statement.name, statement.type)
        return Success("ok", statement)
    }
}

class AssignmentAnalyzer : StatementAnalyzer<Assignment> {
    override fun analyze(statement: Assignment, context: SemanticContext, analyzer: SemanticAnalyzer): Result<cnc.ast.Statement> {
        val targetType = context.symbolTable.typeOf(statement.target)
            ?: return Failure("Variable '${statement.target}' no declarada", ErrorType.SEMANTIC)

        val result = analyzer.analyzeExpression(statement.value, context)
        if (result is Failure) return Failure(result.msg, result.type)

        val valueType = (result as Success).data
        if (valueType != targetType)
            return Failure("No se puede asignar '$valueType' a '${statement.target}' de tipo '$targetType'", ErrorType.SEMANTIC)

        return Success("ok", statement)
    }
}

class CallAnalyzer : StatementAnalyzer<Call> {
    override fun analyze(statement: Call, context: SemanticContext, analyzer: SemanticAnalyzer): Result<cnc.ast.Statement> {
        val signature = context.declaredMethods[statement.function]
            ?: return Failure("Función '${statement.function}' no declarada", ErrorType.SEMANTIC)

        if (statement.arguments.size != signature.params.size)
            return Failure(
                "'${statement.function}' espera ${signature.params.size} args, recibió ${statement.arguments.size}",
                ErrorType.SEMANTIC
            )

        for ((arg, param) in statement.arguments.zip(signature.params)) {
            val result = analyzer.analyzeExpression(arg, context)
            if (result is Failure) return Failure(result.msg, result.type)

            val argType = (result as Success).data
            if (param.type != "any" && argType != param.type)
                return Failure(
                    "Argumento '${param.name}': esperaba '${param.type}', recibió '$argType'",
                    ErrorType.SEMANTIC
                )
        }

        return Success("ok", statement)
    }
}
