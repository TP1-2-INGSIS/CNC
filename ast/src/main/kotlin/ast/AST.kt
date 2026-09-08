package cnc.ast

import ast.ExpressionVisitor

// =============================================================================
// Expressions
// =============================================================================

sealed interface Expression {
    fun <R> accept(visitor: ExpressionVisitor<R>): R
}
data class NumberLiteral(val value: Double) : Expression {
    override fun <R> accept(visitor: ExpressionVisitor<R>) = visitor.visit(this)
}
data class StringLiteral(val value: String) : Expression {
    override fun <R> accept(visitor: ExpressionVisitor<R>) = visitor.visit(this)
}
data class Identifier(val name: String) : Expression {
    override fun <R> accept(visitor: ExpressionVisitor<R>) = visitor.visit(this)
}
data class BinaryExpression(
    val left: Expression,
    val operator: String,
    val right: Expression
) : Expression {
    override fun <R> accept(visitor: ExpressionVisitor<R>) = visitor.visit(this)
}
data class UnaryExpression(
    val operator: String,
    val operand: Expression
) : Expression {
    override fun <R> accept(visitor: ExpressionVisitor<R>) = visitor.visit(this)
}

