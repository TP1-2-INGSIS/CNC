package cnc.ast

sealed interface Statement
data class Declaration(
    val name: String,
    val type: String,
    val value: Expression?
) : Statement
data class Assignment(
    val target: String,
    val value: Expression
) : Statement
data class Call(
    val function: String,
    val arguments: List<Expression>
) : Statement

sealed interface Expression
data class NumberLiteral(val value: Double) : Expression
data class StringLiteral(val value: String) : Expression
data class Identifier(val name: String) : Expression
data class BinaryExpression(
    val left: Expression,
    val operator: String,
    val right: Expression
) : Expression
