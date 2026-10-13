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
 * Encapsula la tabla de símbolos (estado mutable / scope), el motor de resolución de tipos
 * y la capacidad de validar sentencias anidadas sin acoplarse al orquestador global.
 */
class SemanticContext(
    val symbolTable: SymbolTable,
    val expressionResolver: ExpressionTypeResolver,
    private val statementDispatcher: (Statement, SemanticContext) -> Result<Unit>
) {
    fun createChildScope(): SemanticContext =
        SemanticContext(symbolTable.createChild(), expressionResolver, statementDispatcher)

    fun resolveExpressionType(expr: Expression): Result<String> =
        expressionResolver.resolve(expr, symbolTable::typeOf)

    fun isFunctionDeclared(name: String): Boolean =
        expressionResolver.isFunctionDeclared(name)

    fun functionReturnType(name: String): String? =
        expressionResolver.functionReturnType(name)

    fun validate(statement: Statement): Result<Unit> =
        statementDispatcher(statement, this)
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
    fun createContext(table: SymbolTable = symbolTable): SemanticContext =
        SemanticContext(table, expressionResolver, ::validate)

    fun analyze(statements: Sequence<Statement>): Sequence<Result<Statement>> = sequence {
        val context = createContext(symbolTable)
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
