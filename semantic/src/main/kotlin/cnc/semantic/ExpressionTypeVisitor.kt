package cnc.semantic

import ast.ExpressionVisitor
import cnc.ast.BinaryExpression
import cnc.ast.Identifier
import cnc.ast.NumberLiteral
import cnc.ast.StringLiteral
import cnc.common.ErrorType
import cnc.common.Failure
import cnc.common.Result
import cnc.common.Success
import cnc.common.flatMap

class ExpressionTypeVisitor(
    private val declaredVars: Map<String, String>,
    private val binaryRules: Map<String, BinaryOpResolver>
) : ExpressionVisitor<Result<String>> {

    override fun visit(expr: NumberLiteral): Result<String> =
        Success("ok", "number")

    override fun visit(expr: StringLiteral): Result<String> =
        Success("ok", "string")

    override fun visit(expr: Identifier): Result<String> =
        declaredVars[expr.name]
            ?.let { Success("ok", it) }
            ?: Failure("Variable '${expr.name}' no declarada", ErrorType.SEMANTIC)

    override fun visit(expr: BinaryExpression): Result<String> =
        expr.left.accept(this).flatMap { leftType ->
            expr.right.accept(this).flatMap { rightType ->
                binaryRules[expr.operator]?.resolve(leftType, rightType)
                    ?: Failure("Operador '${expr.operator}' no soportado", ErrorType.SEMANTIC)
            }
        }
}
