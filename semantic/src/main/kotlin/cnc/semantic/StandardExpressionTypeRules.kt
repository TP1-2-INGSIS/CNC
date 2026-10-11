package cnc.semantic

import cnc.ast.*
import cnc.common.ErrorType
import cnc.common.Failure
import cnc.common.Result
import cnc.common.Success
import cnc.common.flatMap
import kotlin.reflect.KClass

fun interface ExpressionTypeRule<T : Expression> {
    fun resolve(expr: T, context: ExpressionTypeContext): Result<String>
}

interface ExpressionTypeContext {
    fun typeOf(name: String): String?
    fun isFunctionDeclared(name: String): Boolean = true
    fun functionReturnType(name: String): String? = null
    fun resolve(expr: Expression): Result<String>
    fun resolveBinary(operator: String, leftType: String, rightType: String): Result<String>
    fun resolveUnary(operator: String, operandType: String): Result<String>
}

object StandardExpressionTypeRules {

    val numberLiteral = ExpressionTypeRule<NumberLiteral> { _, _ ->
        Success("ok", "number")
    }

    val stringLiteral = ExpressionTypeRule<StringLiteral> { _, _ ->
        Success("ok", "string")
    }

    val identifier = ExpressionTypeRule<Identifier> { expr, ctx ->
        ctx.typeOf(expr.name)
            ?.let { Success("ok", it) }
            ?: Failure("Variable '${expr.name}' no declarada", ErrorType.SEMANTIC)
    }

    val binaryExpression = ExpressionTypeRule<BinaryExpression> { expr, ctx ->
        ctx.resolve(expr.left).flatMap { leftType ->
            ctx.resolve(expr.right).flatMap { rightType ->
                ctx.resolveBinary(expr.operator, leftType, rightType)
            }
        }
    }

    val unaryExpression = ExpressionTypeRule<UnaryExpression> { expr, ctx ->
        ctx.resolve(expr.operand).flatMap { operandType ->
            ctx.resolveUnary(expr.operator, operandType)
        }
    }

    val callExpression = ExpressionTypeRule<CallExpression> { expr, ctx ->
        for (arg in expr.arguments) {
            val res = ctx.resolve(arg)
            if (res is Failure) return@ExpressionTypeRule res
        }
        if (!ctx.isFunctionDeclared(expr.function)) {
            return@ExpressionTypeRule Failure("Función no declarada o desconocida: '${expr.function}'", ErrorType.SEMANTIC)
        }
        val returnType = ctx.functionReturnType(expr.function)
            ?: if (expr.function == "readInput" || expr.function == "readEnv") "string" else null

        if (returnType != null) {
            Success("ok", returnType)
        } else {
            Failure("La función '${expr.function}' no retorna un valor y no puede usarse en una expresión", ErrorType.SEMANTIC)
        }
    }

    val booleanLiteral = ExpressionTypeRule<BooleanLiteral> { _, _ ->
        Success("ok", "boolean")
    }

    val v1_0: Map<KClass<out Expression>, ExpressionTypeRule<out Expression>> = mapOf(
        NumberLiteral::class to numberLiteral,
        StringLiteral::class to stringLiteral,
        Identifier::class to identifier,
        BinaryExpression::class to binaryExpression,
        UnaryExpression::class to unaryExpression
    )

    val v1_1: Map<KClass<out Expression>, ExpressionTypeRule<out Expression>> = mapOf(
        NumberLiteral::class to numberLiteral,
        StringLiteral::class to stringLiteral,
        BooleanLiteral::class to booleanLiteral,
        Identifier::class to identifier,
        BinaryExpression::class to binaryExpression,
        UnaryExpression::class to unaryExpression,
        CallExpression::class to callExpression
    )
}
