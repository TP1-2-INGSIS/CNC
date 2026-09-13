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

    val printScript10: Map<KClass<out Expression>, ExpressionTypeRule<out Expression>> = mapOf(
        NumberLiteral::class to numberLiteral,
        StringLiteral::class to stringLiteral,
        Identifier::class to identifier,
        BinaryExpression::class to binaryExpression,
        UnaryExpression::class to unaryExpression
    )
}
