package cnc.semantic

import cnc.ast.*
import cnc.common.Result
import cnc.common.Success
import cnc.common.Failure
import cnc.common.ErrorType

/**
 * Implementación de SemanticContext que envuelve SymbolTable y ExpressionTypeVisitor.
 */
class DefaultSemanticContext(
    private val symbolTable: SymbolTable,
    private val binaryRules: Map<String, BinaryOpResolver>
) : SemanticContext {

    override fun isValidType(type: String): Boolean = symbolTable.isValidType(type)
    override fun isDeclared(name: String): Boolean = symbolTable.isDeclared(name)
    override fun declare(name: String, type: String) = symbolTable.declare(name, type)
    override fun typeOf(name: String): String? = symbolTable.typeOf(name)

    override fun resolveExpressionType(expr: Expression): Result<String> {
        val visitor = ExpressionTypeVisitor(symbolTable.asReadOnly(), binaryRules)
        return expr.accept(visitor)
    }
}

/**
 * Analiza semánticamente una secuencia de GenericStatements,
 * delegando a cada StatementDef su propia validación.
 */
class SemanticAnalyzer(
    private val context: SemanticContext
) {
    fun analyze(statements: Sequence<GenericStatement>): Sequence<Result<GenericStatement>> = sequence {
        for (statement in statements) {
            val result = statement.def.semanticCheck(statement.fields, context)
            yield(when (result) {
                is Success -> Success("ok", statement)
                is Failure -> Failure(result.msg, result.type)
            })
        }
    }
}
