package cnc.semantic

import cnc.ast.Expression
import cnc.common.Result
import cnc.semantic.flow.SemanticFlow

/**
 * Context provided to each [ExpressionRule] during semantic type resolution.
 * Encapsulates identifier type lookup in the active scope and delegates recursive
 * sub-expression and operator resolution back to the [ExpressionTypeResolver].
 */
class ExpressionContext(
    val typeLookup: (String) -> String?,
    val resolver: ExpressionTypeResolver
) {
    fun typeOf(name: String): String? = typeLookup(name)

    fun resolve(expr: Expression): Result<String> =
        resolver.resolve(expr, this)

    fun resolveBinary(operator: String, leftType: String, rightType: String): Result<String> =
        resolver.resolveBinary(operator, leftType, rightType)

    fun resolveUnary(operator: String, operandType: String): Result<String> =
        resolver.resolveUnary(operator, operandType)

    fun isFunctionDeclared(name: String): Boolean =
        resolver.isFunctionDeclared(name)

    fun functionReturnType(name: String): String? =
        resolver.functionReturnType(name)

    fun flow(): SemanticFlow = SemanticFlow.ok()
}

/**
 * Contract for an individual semantic expression typing rule.
 */
fun interface ExpressionRule<T : Expression> {
    fun resolve(expr: T, context: ExpressionContext): Result<String>

    companion object {
        fun <T : Expression> builder(tag: String = ""): ExpressionRuleBuilder<T> =
            ExpressionRuleBuilder(tag)
    }
}

typealias ExpressionTypeRule<T> = ExpressionRule<T>

/**
 * Fluent builder for creating strongly typed [ExpressionRule]s.
 */
class ExpressionRuleBuilder<T : Expression>(val tag: String = "") {
    private var resolveBlock: ((T, ExpressionContext) -> Result<String>)? = null

    fun resolve(block: (T, ExpressionContext) -> Result<String>): ExpressionRuleBuilder<T> = apply {
        this.resolveBlock = block
    }

    fun build(): ExpressionRule<T> {
        val r = checkNotNull(resolveBlock) { "resolve block must be specified for rule '$tag'" }
        return ExpressionRule { expr, ctx -> r(expr, ctx) }
    }
}
