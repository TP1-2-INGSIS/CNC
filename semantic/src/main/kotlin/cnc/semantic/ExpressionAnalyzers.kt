package cnc.semantic

import cnc.ast.BinaryExpression
import cnc.ast.Identifier
import cnc.ast.NumberLiteral
import cnc.ast.StringLiteral
import cnc.common.ErrorType
import cnc.common.Failure
import cnc.common.Result
import cnc.common.Success
import cnc.common.flatMap

class NumberLiteralAnalyzer : ExpressionAnalyzer<NumberLiteral> {
    override fun analyze(expression: NumberLiteral, context: SemanticContext, analyzer: SemanticAnalyzer): Result<String> {
        return Success("ok", "number")
    }
}

class StringLiteralAnalyzer : ExpressionAnalyzer<StringLiteral> {
    override fun analyze(expression: StringLiteral, context: SemanticContext, analyzer: SemanticAnalyzer): Result<String> {
        return Success("ok", "string")
    }
}

class IdentifierAnalyzer : ExpressionAnalyzer<Identifier> {
    override fun analyze(expression: Identifier, context: SemanticContext, analyzer: SemanticAnalyzer): Result<String> {
        return context.symbolTable.typeOf(expression.name)
            ?.let { Success("ok", it) }
            ?: Failure("Variable '${expression.name}' no declarada", ErrorType.SEMANTIC)
    }
}

class BinaryExpressionAnalyzer : ExpressionAnalyzer<BinaryExpression> {
    override fun analyze(expression: BinaryExpression, context: SemanticContext, analyzer: SemanticAnalyzer): Result<String> {
        return analyzer.analyzeExpression(expression.left, context).flatMap { leftType ->
            analyzer.analyzeExpression(expression.right, context).flatMap { rightType ->
                context.binaryRules[expression.operator]?.resolve(leftType, rightType)
                    ?: Failure("Operador '${expression.operator}' no soportado", ErrorType.SEMANTIC)
            }
        }
    }
}
