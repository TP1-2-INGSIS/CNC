package cnc.semantic

import cnc.ast.Expression
import cnc.common.BuiltinMethod
import cnc.common.ErrorType
import cnc.common.Failure
import cnc.common.Result
import kotlin.reflect.KClass

class ExpressionTypeResolver(
    val rules: Map<KClass<out Expression>, ExpressionRule<out Expression>> = emptyMap(),
    val binaryRules: Map<String, BinaryOpResolver> = emptyMap(),
    val unaryRules: Map<String, UnaryOpResolver> = mapOf("-" to TypeResolvers.unaryNumeric("-")),
    val builtins: Map<String, BuiltinMethod> = emptyMap(),
) {
    fun isFunctionDeclared(name: String): Boolean = if (builtins.isEmpty()) true else builtins.containsKey(name)

    fun functionReturnType(name: String): String? = builtins[name]?.returnType

    fun resolve(
        expr: Expression,
        typeLookup: (String) -> String?,
    ): Result<String> = resolve(expr, ExpressionContext(typeLookup, this))

    @Suppress("UNCHECKED_CAST")
    fun resolve(
        expr: Expression,
        context: ExpressionContext,
    ): Result<String> {
        val rule =
            rules[expr::class] as? ExpressionRule<Expression>
                ?: return Failure(
                    "No hay regla de tipos registrada para expresión: ${expr::class.simpleName}",
                    ErrorType.SEMANTIC,
                )

        return rule.resolve(expr, context)
    }

    fun resolveBinary(
        operator: String,
        leftType: String,
        rightType: String,
    ): Result<String> =
        binaryRules[operator]?.resolve(leftType, rightType)
            ?: Failure("Operador '$operator' no soportado", ErrorType.SEMANTIC)

    fun resolveUnary(
        operator: String,
        operandType: String,
    ): Result<String> =
        unaryRules[operator]?.resolve(operandType)
            ?: Failure("Operador unario '$operator' no soportado", ErrorType.SEMANTIC)
}
