package cnc.ast

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

/**
 * A block of statements delimited by braces (`{ ... }`).
 *
 * Introduced in PrintScript 1.1 for the bodies of `if`/`else`.
 * Each block creates a child scope: variables declared inside
 * are not visible outside.
 */
data class BlockStatement(
    val statements: List<Statement>
) : Statement

/**
 * Conditional statement (`if`/`else`) introduced in PrintScript 1.1.
 *
 * The condition must evaluate to a boolean. The `else` block is optional.
 * ```
 * if (condition) {
 *     // thenBlock
 * } else {
 *     // elseBlock
 * }
 * ```
 */
data class IfStatement(
    val condition: Expression,
    val thenBlock: BlockStatement,
    val elseBlock: BlockStatement? = null
) : Statement
