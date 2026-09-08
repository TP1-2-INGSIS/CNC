package cnc.ast

// =============================================================================
// Typed Statements
// =============================================================================

sealed interface Statement

data class Declaration(
    val name: String,
    val type: String,
    val value: Expression? = null,
    val isMutable: Boolean = true
) : Statement

data class Assignment(
    val target: String,
    val value: Expression
) : Statement

data class Call(
    val function: String,
    val arguments: List<Expression>
) : Statement

typealias VariableDeclaration = Declaration
typealias VariableAssignment = Assignment
typealias FunctionCall = Call
