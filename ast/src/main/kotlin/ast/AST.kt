package cnc.ast

interface Expression

data class NumberLiteral(val value: Double) : Expression

data class StringLiteral(val value: String) : Expression

data class Identifier(val name: String) : Expression

data class BinaryExpression(
    val left: Expression,
    val operator: String,
    val right: Expression
) : Expression

data class UnaryExpression(
    val operator: String,
    val operand: Expression
) : Expression
