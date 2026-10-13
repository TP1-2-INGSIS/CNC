package cnc.semantic

import cnc.ast.Assignment
import cnc.ast.BlockStatement
import cnc.ast.Call
import cnc.ast.Declaration
import cnc.ast.IfStatement
import cnc.ast.Statement
import cnc.common.ErrorType
import cnc.common.Failure
import cnc.common.Result
import cnc.common.Success
import kotlin.reflect.KClass

fun interface StatementValidator<T : Statement> {
    fun validate(statement: T, context: SemanticContext): Result<Unit>
}

class DeclarationValidator : StatementValidator<Declaration> {
    override fun validate(statement: Declaration, context: SemanticContext): Result<Unit> {
        val symbolTable = context.symbolTable
        if (symbolTable.isDeclaredInCurrentScope(statement.name)) {
            return Failure("Variable '${statement.name}' ya fue declarada en este ámbito", ErrorType.SEMANTIC)
        }
        if (!symbolTable.isValidType(statement.type)) {
            return Failure("Tipo '${statement.type}' no reconocido", ErrorType.SEMANTIC)
        }
        val value = statement.value
        if (value != null) {
            val exprType = context.resolveExpressionType(value)
            when (exprType) {
                is Failure -> return Failure(exprType.msg, exprType.type)
                is Success -> {
                    if (exprType.data != statement.type) {
                        return Failure("Se esperaba '${statement.type}' pero se obtuvo '${exprType.data}'", ErrorType.SEMANTIC)
                    }
                }
            }
        }
        return symbolTable.declare(statement.name, statement.type, statement.isMutable)
    }
}

class AssignmentValidator : StatementValidator<Assignment> {
    override fun validate(statement: Assignment, context: SemanticContext): Result<Unit> {
        val symbolTable = context.symbolTable
        val targetType = symbolTable.typeOf(statement.target)
            ?: return Failure("Variable '${statement.target}' no declarada", ErrorType.SEMANTIC)

        val isMutable = symbolTable.isMutable(statement.target) ?: true
        if (!isMutable) {
            return Failure("No se puede reasignar la constante '${statement.target}'", ErrorType.SEMANTIC)
        }

        val exprType = context.resolveExpressionType(statement.value)
        return when (exprType) {
            is Failure -> Failure(exprType.msg, exprType.type)
            is Success -> {
                if (exprType.data != targetType) {
                    Failure("No se puede asignar '${exprType.data}' a '${statement.target}' de tipo '$targetType'", ErrorType.SEMANTIC)
                } else {
                    Success("ok", Unit)
                }
            }
        }
    }
}

class CallValidator : StatementValidator<Call> {
    override fun validate(statement: Call, context: SemanticContext): Result<Unit> {
        if (!context.isFunctionDeclared(statement.function)) {
            return Failure("Función desconocida o no declarada: '${statement.function}'", ErrorType.SEMANTIC)
        }
        for (arg in statement.arguments) {
            val res = context.resolveExpressionType(arg)
            if (res is Failure) return Failure(res.msg, res.type)
        }
        return Success("ok", Unit)
    }
}

class BlockValidator : StatementValidator<BlockStatement> {
    override fun validate(statement: BlockStatement, context: SemanticContext): Result<Unit> {
        val childContext = context.createChildScope()
        for (stmt in statement.statements) {
            val res = childContext.validate(stmt)
            if (res is Failure) return res
        }
        return Success("ok", Unit)
    }
}

class IfValidator : StatementValidator<IfStatement> {
    override fun validate(statement: IfStatement, context: SemanticContext): Result<Unit> {
        val condType = context.resolveExpressionType(statement.condition)
        if (condType is Failure) return Failure(condType.msg, condType.type)
        if ((condType as Success).data != "boolean") {
            return Failure("La condición del 'if' debe ser de tipo 'boolean', pero se obtuvo '${condType.data}'", ErrorType.SEMANTIC)
        }

        val thenResult = context.validate(statement.thenBlock)
        if (thenResult is Failure) return thenResult

        val elseBlock = statement.elseBlock
        if (elseBlock != null) {
            val elseResult = context.validate(elseBlock)
            if (elseResult is Failure) return elseResult
        }

        return Success("ok", Unit)
    }
}

object StandardStatementValidators {
    val declaration = DeclarationValidator()
    val assignment = AssignmentValidator()
    val call = CallValidator()
    val block = BlockValidator()
    val ifValidator = IfValidator()

    val v1_0: Map<KClass<out Statement>, StatementValidator<*>> = mapOf(
        Declaration::class to declaration,
        Assignment::class to assignment,
        Call::class to call
    )

    val v1_1: Map<KClass<out Statement>, StatementValidator<*>> = mapOf(
        Declaration::class to declaration,
        Assignment::class to assignment,
        Call::class to call,
        BlockStatement::class to block,
        IfStatement::class to ifValidator
    )
}
