package cnc.ast

interface Expression

data class NumberLiteral(val value: Double) : Expression

data class StringLiteral(val value: String) : Expression

/**
 * Boolean literal for PrintScript 1.1 (`true`, `false`).
 */
data class BooleanLiteral(val value: Boolean) : Expression

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

/**
 * Function call used as an expression (returns a value).
 *
 * Introduced in PrintScript 1.1 to support `readInput()` and `readEnv()`
 * in expression contexts, e.g.:
 * ```
 * let name: string = readInput("Enter name:");
 * ```
 *
 * This is separate from [Call] (which is a [Statement]) to preserve
 * the existing statement-level call semantics for `println(...)`.
 */
data class CallExpression(
    val function: String,
    val arguments: List<Expression>
) : Expression
