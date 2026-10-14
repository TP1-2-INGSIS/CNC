package cnc.semantic

import cnc.ast.BinaryExpression
import cnc.ast.BooleanLiteral
import cnc.ast.CallExpression
import cnc.ast.Identifier
import cnc.ast.NumberLiteral
import cnc.ast.StringLiteral
import cnc.ast.UnaryExpression
import cnc.common.ErrorType
import cnc.common.Failure
import cnc.common.Success

object StandardExpressionRules {
    val numberLiteral =
        ExpressionRule
            .builder<NumberLiteral>("numberLiteral")
            .resolve { _, _ -> Success("ok", "number") }
            .build()

    val stringLiteral =
        ExpressionRule
            .builder<StringLiteral>("stringLiteral")
            .resolve { _, _ -> Success("ok", "string") }
            .build()

    val booleanLiteral =
        ExpressionRule
            .builder<BooleanLiteral>("booleanLiteral")
            .resolve { _, _ -> Success("ok", "boolean") }
            .build()

    val identifier =
        ExpressionRule
            .builder<Identifier>("identifier")
            .resolve { expr, ctx ->
                ctx
                    .flow()
                    .expect(ctx.typeOf(expr.name) != null) { "Variable '${expr.name}' no declarada" }
                    .then { ctx.typeOf(expr.name)!! }
            }.build()

    val binaryExpression =
        ExpressionRule
            .builder<BinaryExpression>("binaryExpression")
            .resolve { expr, ctx ->
                var left = ""
                var right = ""
                ctx
                    .flow()
                    .step({ ctx.resolve(expr.left) }) { left = it }
                    .step({ ctx.resolve(expr.right) }) { right = it }
                    .thenResult { ctx.resolveBinary(expr.operator, left, right) }
            }.build()

    val unaryExpression =
        ExpressionRule
            .builder<UnaryExpression>("unaryExpression")
            .resolve { expr, ctx ->
                var operand = ""
                ctx
                    .flow()
                    .step({ ctx.resolve(expr.operand) }) { operand = it }
                    .thenResult { ctx.resolveUnary(expr.operator, operand) }
            }.build()

    val callExpression =
        ExpressionRule
            .builder<CallExpression>("callExpression")
            .resolve { expr, ctx ->
                ctx
                    .flow()
                    .expect(ctx.isFunctionDeclared(expr.function)) {
                        "Función no declarada o desconocida: '${expr.function}'"
                    }.stepAll(expr.arguments, ctx::resolve)
                    .thenResult {
                        ctx.functionReturnType(expr.function)?.let { Success("ok", it) }
                            ?: Failure(
                                "La función '${expr.function}' no retorna un valor y no puede usarse en una expresión",
                                ErrorType.SEMANTIC,
                            )
                    }
            }.build()
}
