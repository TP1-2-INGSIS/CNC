package cnc.semantic

import cnc.ast.Expression
import cnc.common.BuiltinMethod
import cnc.common.ErrorType
import cnc.common.Failure
import cnc.common.Result
import kotlin.reflect.KClass

class ExpressionTypeResolver(
    private val rules: Map<KClass<out Expression>, ExpressionTypeRule<out Expression>>,
    private val typeLookup: (String) -> String?,
    private val binaryRules: Map<String, BinaryOpResolver>,
    private val unaryRules: Map<String, UnaryOpResolver> = emptyMap(),
    private val builtins: Map<String, BuiltinMethod> = emptyMap()
) : ExpressionTypeContext {

    constructor(
        rules: Map<KClass<out Expression>, ExpressionTypeRule<out Expression>>,
        declaredVars: Map<String, String>,
        binaryRules: Map<String, BinaryOpResolver>,
        unaryRules: Map<String, UnaryOpResolver> = emptyMap(),
        builtins: Map<String, BuiltinMethod> = emptyMap()
    ) : this(
        rules = rules,
        typeLookup = { declaredVars[it] },
        binaryRules = binaryRules,
        unaryRules = unaryRules,
        builtins = builtins
    )

    override fun typeOf(name: String): String? = typeLookup(name)

    override fun isFunctionDeclared(name: String): Boolean =
        if (builtins.isEmpty()) true else builtins.containsKey(name)

    override fun functionReturnType(name: String): String? =
        builtins[name]?.returnType

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
