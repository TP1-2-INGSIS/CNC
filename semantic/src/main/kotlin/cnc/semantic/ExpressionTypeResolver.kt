package cnc.semantic

import cnc.ast.BinaryExpression
import cnc.ast.Expression
import cnc.ast.Identifier
import cnc.ast.NumberLiteral
import cnc.ast.StringLiteral
import cnc.ast.UnaryExpression
import cnc.common.ErrorType
import cnc.common.Failure
import cnc.common.Result
import cnc.common.Success
import cnc.common.flatMap

class ExpressionTypeResolver(
    private val declaredVars: Map<String, String>,
    private val binaryRules: Map<String, BinaryOpResolver>
) {
    fun resolve(expr: Expression): Result<String> = when (expr) {
        is NumberLiteral ->
            Success("ok", "number")

        is StringLiteral ->
            Success("ok", "string")

        is Identifier ->
            declaredVars[expr.name]
                ?.let { Success("ok", it) }
                ?: Failure("Variable '${expr.name}' no declarada", ErrorType.SEMANTIC)

        is BinaryExpression ->
            resolve(expr.left).flatMap { leftType ->
                resolve(expr.right).flatMap { rightType ->
                    binaryRules[expr.operator]?.resolve(leftType, rightType)
                        ?: Failure("Operador '${expr.operator}' no soportado", ErrorType.SEMANTIC)
                }
            }

        is UnaryExpression ->
            resolve(expr.operand).flatMap { operandType ->
                if (expr.operator == "-" && operandType == "number") {
                    Success("ok", "number")
                } else {
                    Failure("Operador unario '${expr.operator}' no soportado para tipo '$operandType'", ErrorType.SEMANTIC)
                }
            }
    }
}
