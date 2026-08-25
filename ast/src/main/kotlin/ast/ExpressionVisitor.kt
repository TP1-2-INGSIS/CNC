package ast

import cnc.ast.BinaryExpression
import cnc.ast.Identifier
import cnc.ast.NumberLiteral
import cnc.ast.StringLiteral

interface ExpressionVisitor<R> {
    fun visit(expr: NumberLiteral): R
    fun visit(expr: StringLiteral): R
    fun visit(expr: Identifier): R
    fun visit(expr: BinaryExpression): R
}
