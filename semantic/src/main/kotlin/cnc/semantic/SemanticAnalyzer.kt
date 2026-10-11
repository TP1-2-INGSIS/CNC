package cnc.semantic

import cnc.ast.Expression
import cnc.ast.Statement
import cnc.common.ErrorType
import cnc.common.Failure
import cnc.common.Result
import cnc.common.Success
import kotlin.reflect.KClass

/**
 * Contexto de ejecución pasado a cada [StatementValidator] durante el análisis semántico.
 * Encapsula la tabla de símbolos (estado mutable / scope) y el analizador semántico (motor).
 */
data class SemanticContext(
    val symbolTable: SymbolTable,
    val analyzer: SemanticAnalyzer
) {
    fun createChildScope(): SemanticContext =
        copy(symbolTable = symbolTable.createChild())

    fun resolveExpressionType(expr: Expression): Result<String> =
        analyzer.resolveExpressionType(expr, symbolTable)
}

/**
 * Orquestador principal de la fase de análisis semántico.
 * Contiene el motor de resolución de expresiones, la tabla de símbolos y el registro de validadores de sentencias.
 */
class SemanticAnalyzer(
    val expressionResolver: ExpressionTypeResolver,
    val statementValidators: Map<KClass<out Statement>, StatementValidator<*>> = StandardStatementValidators.v1_1,
    val symbolTable: SymbolTable = SymbolTable(setOf("number", "string", "boolean"))
) {
    fun analyze(statements: Sequence<Statement>): Sequence<Result<Statement>> = sequence {
        val context = SemanticContext(symbolTable, this@SemanticAnalyzer)
        for (statement in statements) {
            val result = validate(statement, context)
            yield(when (result) {
                is Success -> Success("ok", statement)
                is Failure -> Failure(result.msg, result.type)
            })
        }
    }

    @Suppress("UNCHECKED_CAST")
    fun validate(statement: Statement, context: SemanticContext): Result<Unit> {
        val validator = statementValidators[statement::class] as? StatementValidator<Statement>
            ?: return Failure("No hay validador semántico registrado para la sentencia: ${statement::class.simpleName}", ErrorType.SEMANTIC)

        return validator.validate(statement, context)
    }

    fun resolveExpressionType(expr: Expression, table: SymbolTable = symbolTable): Result<String> =
        expressionResolver.resolve(expr, table::typeOf)

    fun isFunctionDeclared(name: String): Boolean =
        expressionResolver.isFunctionDeclared(name)

    fun functionReturnType(name: String): String? =
        expressionResolver.functionReturnType(name)
}
