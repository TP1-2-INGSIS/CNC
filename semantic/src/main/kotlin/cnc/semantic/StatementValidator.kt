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
    fun validate(
        statement: T,
        context: SemanticContext,
    ): Result<Unit>

    companion object {
        fun <T : Statement> builder(tag: String = ""): StatementValidatorBuilder<T> = StatementValidatorBuilder(tag)
    }
}

class StatementValidatorBuilder<T : Statement>(
    val tag: String = "",
) {
    private var validateBlock: ((T, SemanticContext) -> Result<Unit>)? = null

    fun validate(block: (T, SemanticContext) -> Result<Unit>): StatementValidatorBuilder<T> =
        apply {
            this.validateBlock = block
        }

    fun build(): StatementValidator<T> {
        val block = checkNotNull(validateBlock) { "validate block must be specified for validator '$tag'" }
        return StatementValidator { stmt, ctx -> block(stmt, ctx) }
    }
}

class DeclarationValidator : StatementValidator<Declaration> {
    override fun validate(
        statement: Declaration,
        context: SemanticContext,
    ): Result<Unit> {
        val symbolTable = context.symbolTable
        var exprType: String? = null
        return context
            .flow()
            .expect(!symbolTable.isDeclaredInCurrentScope(statement.name)) {
                "Variable '${statement.name}' ya fue declarada en este ámbito"
            }.expect(symbolTable.isValidType(statement.type)) {
                "Tipo '${statement.type}' no reconocido"
            }.optionalStep(statement.value, context::resolveExpressionType) { exprType = it }
            .expect(exprType == null || exprType == statement.type) {
                "Se esperaba '${statement.type}' pero se obtuvo '$exprType'"
            }.thenResult {
                symbolTable.declare(statement.name, statement.type, statement.isMutable)
            }
    }
}

class AssignmentValidator : StatementValidator<Assignment> {
    override fun validate(
        statement: Assignment,
        context: SemanticContext,
    ): Result<Unit> {
        val symbolTable = context.symbolTable
        var targetType = ""
        var exprType = ""

        return context
            .flow()
            .step({
                symbolTable.typeOf(statement.target)?.let { Success("ok", it) }
                    ?: Failure("Variable '${statement.target}' no declarada", ErrorType.SEMANTIC)
            }) { targetType = it }
            .expect(symbolTable.isMutable(statement.target) != false) {
                "No se puede reasignar la constante '${statement.target}'"
            }.step({ context.resolveExpressionType(statement.value) }) { exprType = it }
            .expect(exprType == targetType) {
                "No se puede asignar '$exprType' a '${statement.target}' de tipo '$targetType'"
            }.thenOk()
    }
}

class CallValidator : StatementValidator<Call> {
    override fun validate(
        statement: Call,
        context: SemanticContext,
    ): Result<Unit> =
        context
            .flow()
            .expect(context.isFunctionDeclared(statement.function)) {
                "Función desconocida o no declarada: '${statement.function}'"
            }.stepAll(statement.arguments, context::resolveExpressionType)
            .thenOk()
}

class BlockValidator : StatementValidator<BlockStatement> {
    override fun validate(
        statement: BlockStatement,
        context: SemanticContext,
    ): Result<Unit> = context.validateInChildScope(statement.statements)
}

class IfValidator : StatementValidator<IfStatement> {
    override fun validate(
        statement: IfStatement,
        context: SemanticContext,
    ): Result<Unit> {
        var condType = ""
        return context
            .flow()
            .step({ context.resolveExpressionType(statement.condition) }) { condType = it }
            .expect(condType == "boolean") {
                "La condición del 'if' debe ser de tipo 'boolean', pero se obtuvo '$condType'"
            }.step({ context.validateInChildScope(statement.thenBlock.statements) })
            .optionalStep(statement.elseBlock) { elseBlock ->
                context.validateInChildScope(elseBlock.statements)
            }.thenOk()
    }
}

object StandardStatementValidators {
    val declaration = DeclarationValidator()
    val assignment = AssignmentValidator()
    val call = CallValidator()
    val block = BlockValidator()
    val ifValidator = IfValidator()

    val v1_0: Map<KClass<out Statement>, StatementValidator<*>> =
        mapOf(
            Declaration::class to declaration,
            Assignment::class to assignment,
            Call::class to call,
        )

    val v1_1: Map<KClass<out Statement>, StatementValidator<*>> =
        mapOf(
            Declaration::class to declaration,
            Assignment::class to assignment,
            Call::class to call,
            BlockStatement::class to block,
            IfStatement::class to ifValidator,
        )
}
