package cnc.semantic

import cnc.ast.Expression
import cnc.ast.Statement
import cnc.common.BuiltinMethod
import cnc.common.ErrorType
import cnc.common.Failure
import cnc.common.Result
import cnc.common.Success
import kotlin.reflect.KClass

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
    fun isFunctionDeclared(name: String): Boolean = true
    fun functionReturnType(name: String): String? = null
}

/**
 * Implementación de SemanticContext que envuelve SymbolTable y ExpressionTypeResolver.
 */
class DefaultSemanticContext(
    val symbolTable: SymbolTable,
    val binaryRules: Map<String, BinaryOpResolver>,
    val unaryRules: Map<String, UnaryOpResolver> = mapOf("-" to TypeResolvers.unaryNumeric("-")),
    val expressionRules: Map<KClass<out Expression>, ExpressionTypeRule<out Expression>> = StandardExpressionTypeRules.v1_1,
    val builtins: Map<String, BuiltinMethod> = emptyMap()
) : SemanticContext {

    private val expressionTypeResolver = ExpressionTypeResolver(
        rules = expressionRules,
        typeLookup = symbolTable::typeOf,
        binaryRules = binaryRules,
        unaryRules = unaryRules,
        builtins = builtins
    )

    fun copy(
        symbolTable: SymbolTable = this.symbolTable,
        binaryRules: Map<String, BinaryOpResolver> = this.binaryRules,
        unaryRules: Map<String, UnaryOpResolver> = this.unaryRules,
        expressionRules: Map<KClass<out Expression>, ExpressionTypeRule<out Expression>> = this.expressionRules,
        builtins: Map<String, BuiltinMethod> = this.builtins
    ): DefaultSemanticContext = DefaultSemanticContext(
        symbolTable = symbolTable,
        binaryRules = binaryRules,
        unaryRules = unaryRules,
        expressionRules = expressionRules,
        builtins = builtins
    )

    override fun isValidType(type: String): Boolean = symbolTable.isValidType(type)
    override fun isDeclared(name: String): Boolean = symbolTable.isDeclared(name)
    override fun isDeclaredInCurrentScope(name: String): Boolean = symbolTable.isDeclaredInCurrentScope(name)
    override fun declare(name: String, type: String, isMutable: Boolean): Result<Unit> =
        symbolTable.declare(name, type, isMutable)
    override fun typeOf(name: String): String? = symbolTable.typeOf(name)
    override fun isMutable(name: String): Boolean? = symbolTable.isMutable(name)

    override fun isFunctionDeclared(name: String): Boolean =
        if (builtins.isEmpty()) true else builtins.containsKey(name)

    override fun functionReturnType(name: String): String? =
        builtins[name]?.returnType

    override fun resolveExpressionType(expr: Expression): Result<String> =
        expressionTypeResolver.resolve(expr)

    override fun createChildScope(): SemanticContext = DefaultSemanticContext(
        symbolTable = symbolTable.createChild(),
        binaryRules = binaryRules,
        unaryRules = unaryRules,
        expressionRules = expressionRules,
        builtins = builtins
    )
}

/**
 * Analiza semánticamente una secuencia de Statements tipados utilizando StatementValidators desacoplados.
 */
class SemanticAnalyzer(
    private val context: SemanticContext,
    private val statementValidators: Map<KClass<out Statement>, StatementValidator<*>> = StandardStatementValidators.v1_1
) {
    fun analyze(statements: Sequence<Statement>): Sequence<Result<Statement>> = sequence {
        for (statement in statements) {
            val result = validate(statement, context)
            yield(when (result) {
                is Success -> Success("ok", statement)
                is Failure -> Failure(result.msg, result.type)
            })
        }
    }

    @Suppress("UNCHECKED_CAST")
    fun validate(statement: Statement, ctx: SemanticContext = context): Result<Unit> {
        val validator = statementValidators[statement::class] as? StatementValidator<Statement>
            ?: return Failure("No hay validador semántico registrado para la sentencia: ${statement::class.simpleName}", ErrorType.SEMANTIC)

        return validator.validate(statement, ctx, this)
    }
}
