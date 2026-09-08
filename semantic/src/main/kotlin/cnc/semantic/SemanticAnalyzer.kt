package cnc.semantic

import cnc.ast.Assignment
import cnc.ast.Call
import cnc.ast.Declaration
import cnc.ast.Expression
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
    fun declare(name: String, type: String)
    fun typeOf(name: String): String?
    fun resolveExpressionType(expr: Expression): Result<String>
}

/**
 * Implementación de SemanticContext que envuelve SymbolTable y ExpressionTypeResolver.
 */
class DefaultSemanticContext(
    private val symbolTable: SymbolTable,
    private val binaryRules: Map<String, BinaryOpResolver>,
    private val expressionRules: Map<kotlin.reflect.KClass<out Expression>, ExpressionTypeRule<out Expression>> = StandardExpressionTypeRules.printScript10
) : SemanticContext {

    override fun isValidType(type: String): Boolean = symbolTable.isValidType(type)
    override fun isDeclared(name: String): Boolean = symbolTable.isDeclared(name)
    override fun declare(name: String, type: String) = symbolTable.declare(name, type)
    override fun typeOf(name: String): String? = symbolTable.typeOf(name)

    override fun resolveExpressionType(expr: Expression): Result<String> {
        val resolver = ExpressionTypeResolver(expressionRules, symbolTable.asReadOnly(), binaryRules)
        return resolver.resolve(expr)
    }
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
    }

    private fun checkDeclaration(decl: Declaration): Result<Unit> {
        if (context.isDeclared(decl.name)) {
            return Failure("Variable '${decl.name}' ya fue declarada", ErrorType.SEMANTIC)
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
        context.declare(decl.name, decl.type)
        return Success("ok", Unit)
    }

    private fun checkAssignment(assign: Assignment): Result<Unit> {
        val targetType = context.typeOf(assign.target)
            ?: return Failure("Variable '${assign.target}' no declarada", ErrorType.SEMANTIC)

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
