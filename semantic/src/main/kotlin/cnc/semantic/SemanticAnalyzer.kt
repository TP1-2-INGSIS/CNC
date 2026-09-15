package cnc.semantic

import cnc.ast.Assignment
import cnc.ast.BlockStatement
import cnc.ast.Call
import cnc.ast.Declaration
import cnc.ast.Expression
import cnc.ast.IfStatement
import cnc.ast.Statement
import cnc.common.ErrorType
import cnc.common.Failure
import cnc.common.Result
import cnc.common.Success

/**
 * Contexto accesible durante la fase de análisis semántico.
 */
interface SemanticContext {
    fun isValidType(type: String): Boolean
    fun isDeclared(name: String): Boolean
    fun isDeclaredInCurrentScope(name: String): Boolean
    fun declare(name: String, type: String, isMutable: Boolean = true): Result<Unit>
    fun typeOf(name: String): String?
    fun isMutable(name: String): Boolean?
    fun resolveExpressionType(expr: Expression): Result<String>
    fun createChildScope(): SemanticContext
}

/**
 * Implementación de SemanticContext que envuelve SymbolTable y ExpressionTypeResolver.
 */
class DefaultSemanticContext(
    private val symbolTable: SymbolTable,
    private val binaryRules: Map<String, BinaryOpResolver>,
    private val unaryRules: Map<String, UnaryOpResolver> = mapOf("-" to TypeResolvers.unaryNumeric("-")),
    private val expressionRules: Map<kotlin.reflect.KClass<out Expression>, ExpressionTypeRule<out Expression>> = StandardExpressionTypeRules.printScript10
) : SemanticContext {

    override fun isValidType(type: String): Boolean = symbolTable.isValidType(type)
    override fun isDeclared(name: String): Boolean = symbolTable.isDeclared(name)
    override fun isDeclaredInCurrentScope(name: String): Boolean = symbolTable.isDeclaredInCurrentScope(name)
    override fun declare(name: String, type: String, isMutable: Boolean): Result<Unit> =
        symbolTable.declare(name, type, isMutable)
    override fun typeOf(name: String): String? = symbolTable.typeOf(name)
    override fun isMutable(name: String): Boolean? = symbolTable.isMutable(name)

    override fun resolveExpressionType(expr: Expression): Result<String> {
        val resolver = ExpressionTypeResolver(expressionRules, symbolTable.asReadOnly(), binaryRules, unaryRules)
        return resolver.resolve(expr)
    }

    override fun createChildScope(): SemanticContext = DefaultSemanticContext(
        symbolTable = symbolTable.createChild(),
        binaryRules = binaryRules,
        unaryRules = unaryRules,
        expressionRules = expressionRules
    )
}

/**
 * Analiza semánticamente una secuencia de Statements tipados.
 */
class SemanticAnalyzer(
    private val context: SemanticContext
) {
    fun analyze(statements: Sequence<Statement>): Sequence<Result<Statement>> = sequence {
        for (statement in statements) {
            val result = check(statement)
            yield(when (result) {
                is Success -> Success("ok", statement)
                is Failure -> Failure(result.msg, result.type)
            })
        }
    }

    private fun check(statement: Statement): Result<Unit> = when (statement) {
        is Declaration -> checkDeclaration(statement)
        is Assignment -> checkAssignment(statement)
        is Call -> checkCall(statement)
        is BlockStatement -> checkBlock(statement)
        is IfStatement -> checkIf(statement)
    }

    private fun checkBlock(block: BlockStatement): Result<Unit> {
        val childContext = context.createChildScope()
        val childAnalyzer = SemanticAnalyzer(childContext)
        for (stmt in block.statements) {
            val result = childAnalyzer.check(stmt)
            if (result is Failure) return result
        }
        return Success("ok", Unit)
    }

    private fun checkIf(ifStmt: IfStatement): Result<Unit> {
        val condType = context.resolveExpressionType(ifStmt.condition)
        if (condType is Failure) return Failure(condType.msg, condType.type)
        if ((condType as Success).data != "boolean") {
            return Failure("La condición del 'if' debe ser de tipo 'boolean', pero se obtuvo '${condType.data}'", ErrorType.SEMANTIC)
        }

        val thenResult = checkBlock(ifStmt.thenBlock)
        if (thenResult is Failure) return thenResult

        val elseBlock = ifStmt.elseBlock
        if (elseBlock != null) {
            val elseResult = checkBlock(elseBlock)
            if (elseResult is Failure) return elseResult
        }

        return Success("ok", Unit)
    }

    private fun checkDeclaration(decl: Declaration): Result<Unit> {
        if (context.isDeclaredInCurrentScope(decl.name)) {
            return Failure("Variable '${decl.name}' ya fue declarada en este ámbito", ErrorType.SEMANTIC)
        }
        if (!context.isValidType(decl.type)) {
            return Failure("Tipo '${decl.type}' no reconocido", ErrorType.SEMANTIC)
        }
        val value = decl.value
        if (value != null) {
            val exprType = context.resolveExpressionType(value)
            when (exprType) {
                is Failure -> return Failure(exprType.msg, exprType.type)
                is Success -> {
                    if (exprType.data != decl.type) {
                        return Failure("Se esperaba '${decl.type}' pero se obtuvo '${exprType.data}'", ErrorType.SEMANTIC)
                    }
                }
            }
        }
        return context.declare(decl.name, decl.type, decl.isMutable)
    }

    private fun checkAssignment(assign: Assignment): Result<Unit> {
        val targetType = context.typeOf(assign.target)
            ?: return Failure("Variable '${assign.target}' no declarada", ErrorType.SEMANTIC)

        val isMutable = context.isMutable(assign.target) ?: true
        if (!isMutable) {
            return Failure("No se puede reasignar la constante '${assign.target}'", ErrorType.SEMANTIC)
        }

        val exprType = context.resolveExpressionType(assign.value)
        return when (exprType) {
            is Failure -> Failure(exprType.msg, exprType.type)
            is Success -> {
                if (exprType.data != targetType) {
                    Failure("No se puede asignar '${exprType.data}' a '${assign.target}' de tipo '$targetType'", ErrorType.SEMANTIC)
                } else {
                    Success("ok", Unit)
                }
            }
        }
    }

    private fun checkCall(call: Call): Result<Unit> {
        for (arg in call.arguments) {
            val res = context.resolveExpressionType(arg)
            if (res is Failure) return Failure(res.msg, res.type)
        }
        return Success("ok", Unit)
    }
}
