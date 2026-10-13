package cnc.semantic

import cnc.ast.Expression
import cnc.ast.Statement
import cnc.common.BuiltinMethod
import cnc.common.ErrorType
import cnc.common.Failure
import cnc.common.Result
import cnc.common.Success
import cnc.semantic.flow.SemanticFlow
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

    fun validateInChildScope(statements: Iterable<Statement>): Result<Unit> {
        val child = createChildScope()
        for (stmt in statements) {
            val res = child.validate(stmt)
            if (res is Failure) return res
        }
        return Success("ok", Unit)
    }

    fun flow(): SemanticFlow = SemanticFlow.ok()
}

/**
 * Orquestador principal de la fase de análisis semántico.
 * Contiene el motor de resolución de expresiones, la tabla de símbolos y el registro de validadores de sentencias.
 */
class SemanticAnalyzer(
    val expressionResolver: ExpressionTypeResolver,
    val statementValidators: Map<KClass<out Statement>, StatementValidator<*>> = emptyMap(),
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

    companion object {
        fun builder(): SemanticAnalyzerBuilder = SemanticAnalyzerBuilder()
    }
}

/**
 * Fluent builder for assembling a [SemanticAnalyzer] with customizable rules and symbol tables.
 */
class SemanticAnalyzerBuilder {
    private var symbolTable: SymbolTable = SymbolTable(setOf("number", "string", "boolean"))
    private var statementValidators: MutableMap<KClass<out Statement>, StatementValidator<*>> = mutableMapOf()
    private var expressionRules: MutableMap<KClass<out Expression>, ExpressionRule<*>> = mutableMapOf()
    private var binaryRules: MutableMap<String, BinaryOpResolver> = mutableMapOf()
    private var unaryRules: MutableMap<String, UnaryOpResolver> = mutableMapOf("-" to TypeResolvers.unaryNumeric("-"))
    private var builtins: MutableMap<String, BuiltinMethod> = mutableMapOf()

    fun withSymbolTable(table: SymbolTable): SemanticAnalyzerBuilder = apply {
        this.symbolTable = table
    }

    fun withValidTypes(types: Set<String>): SemanticAnalyzerBuilder = apply {
        this.symbolTable = SymbolTable(types)
    }

    fun registerStatementValidator(kClass: KClass<out Statement>, validator: StatementValidator<*>): SemanticAnalyzerBuilder = apply {
        this.statementValidators[kClass] = validator
    }

    fun registerStatementValidators(validators: Map<KClass<out Statement>, StatementValidator<*>>): SemanticAnalyzerBuilder = apply {
        this.statementValidators.putAll(validators)
    }

    fun setStatementValidators(validators: Map<KClass<out Statement>, StatementValidator<*>>): SemanticAnalyzerBuilder = apply {
        this.statementValidators = validators.toMutableMap()
    }

    fun registerExpressionRule(kClass: KClass<out Expression>, rule: ExpressionRule<*>): SemanticAnalyzerBuilder = apply {
        this.expressionRules[kClass] = rule
    }

    fun registerExpressionRules(rules: Map<KClass<out Expression>, ExpressionRule<*>>): SemanticAnalyzerBuilder = apply {
        this.expressionRules.putAll(rules)
    }

    fun setExpressionRules(rules: Map<KClass<out Expression>, ExpressionRule<*>>): SemanticAnalyzerBuilder = apply {
        this.expressionRules = rules.toMutableMap()
    }

    fun registerBinaryOp(operator: String, resolver: BinaryOpResolver): SemanticAnalyzerBuilder = apply {
        this.binaryRules[operator] = resolver
    }

    fun registerBinaryOps(ops: Map<String, BinaryOpResolver>): SemanticAnalyzerBuilder = apply {
        this.binaryRules.putAll(ops)
    }

    fun registerUnaryOp(operator: String, resolver: UnaryOpResolver): SemanticAnalyzerBuilder = apply {
        this.unaryRules[operator] = resolver
    }

    fun registerUnaryOps(ops: Map<String, UnaryOpResolver>): SemanticAnalyzerBuilder = apply {
        this.unaryRules.putAll(ops)
    }

    fun registerBuiltin(name: String, method: BuiltinMethod): SemanticAnalyzerBuilder = apply {
        this.builtins[name] = method
    }

    fun registerBuiltins(builtins: Map<String, BuiltinMethod>): SemanticAnalyzerBuilder = apply {
        this.builtins.putAll(builtins)
    }

    fun build(): SemanticAnalyzer {
        val resolver = ExpressionTypeResolver(
            rules = expressionRules,
            binaryRules = binaryRules,
            unaryRules = unaryRules,
            builtins = builtins
        )
        return SemanticAnalyzer(
            expressionResolver = resolver,
            statementValidators = statementValidators,
            symbolTable = symbolTable
        )
    }
}
