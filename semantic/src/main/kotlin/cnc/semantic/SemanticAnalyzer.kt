package cnc.semantic

import cnc.ast.Expression
import cnc.ast.Statement
import cnc.common.ErrorType
import cnc.common.Failure
import cnc.common.Result
import kotlin.reflect.KClass

data class Param(val name: String, val type: String)
data class MethodSignature(val params: List<Param>, val returnType: String)

interface StatementAnalyzer<T : Statement> {
    fun analyze(statement: T, context: SemanticContext, analyzer: SemanticAnalyzer): Result<Statement>
}

interface ExpressionAnalyzer<T : Expression> {
    fun analyze(expression: T, context: SemanticContext, analyzer: SemanticAnalyzer): Result<String>
}

class SemanticContext(
    val symbolTable: SymbolTable,
    val binaryRules: Map<String, BinaryOpResolver>,
    val declaredMethods: Map<String, MethodSignature> = mapOf(
        "println" to MethodSignature(listOf(Param("value", "any")), "void")
    )
)

class SemanticAnalyzer(
    private val statementAnalyzers: Map<KClass<out Statement>, StatementAnalyzer<out Statement>>,
    private val expressionAnalyzers: Map<KClass<out Expression>, ExpressionAnalyzer<out Expression>>
) {
    fun analyze(statements: Sequence<Statement>, context: SemanticContext): Sequence<Result<Statement>> = sequence {
        for (statement in statements) {
            yield(analyzeStatement(statement, context))
        }
    }

    @Suppress("UNCHECKED_CAST")
    fun analyzeStatement(statement: Statement, context: SemanticContext): Result<Statement> {
        val analyzer = statementAnalyzers[statement::class] as? StatementAnalyzer<Statement>
            ?: return Failure("No analyzer for ${statement::class.simpleName}", ErrorType.SEMANTIC)
        return analyzer.analyze(statement, context, this)
    }

    @Suppress("UNCHECKED_CAST")
    fun analyzeExpression(expression: Expression, context: SemanticContext): Result<String> {
        val analyzer = expressionAnalyzers[expression::class] as? ExpressionAnalyzer<Expression>
            ?: return Failure("No analyzer for ${expression::class.simpleName}", ErrorType.SEMANTIC)
        return analyzer.analyze(expression, context, this)
    }
}
