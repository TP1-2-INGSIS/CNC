package cnc.semantic

import cnc.ast.Expression
import cnc.common.ErrorType
import cnc.common.Failure
import cnc.common.Result
import kotlin.reflect.KClass

class ExpressionTypeResolver(
    private val rules: Map<KClass<out Expression>, ExpressionTypeRule<out Expression>>,
    private val declaredVars: Map<String, String>,
    private val binaryRules: Map<String, BinaryOpResolver>,
    private val unaryRules: Map<String, UnaryOpResolver> = emptyMap()
) : ExpressionTypeContext {

    override fun typeOf(name: String): String? = declaredVars[name]

    override fun resolveBinary(operator: String, leftType: String, rightType: String): Result<String> {
        return binaryRules[operator]?.resolve(leftType, rightType)
            ?: Failure("Operador '$operator' no soportado", ErrorType.SEMANTIC)
    }

    override fun resolveUnary(operator: String, operandType: String): Result<String> {
        return unaryRules[operator]?.resolve(operandType)
            ?: Failure("Operador unario '$operator' no soportado", ErrorType.SEMANTIC)
    }

    @Suppress("UNCHECKED_CAST")
    override fun resolve(expr: Expression): Result<String> {
        val rule = rules[expr::class] as? ExpressionTypeRule<Expression>
            ?: return Failure("No hay regla de tipos registrada para expresión: ${expr::class.simpleName}", ErrorType.SEMANTIC)

        return rule.resolve(expr, this)
    }
}
