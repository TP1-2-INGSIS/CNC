package cnc.formatter

import cnc.ast.Assignment
import cnc.ast.BinaryExpression
import cnc.ast.BlockStatement
import cnc.ast.BooleanLiteral
import cnc.ast.Call
import cnc.ast.CallExpression
import cnc.ast.Declaration
import cnc.ast.Expression
import cnc.ast.Identifier
import cnc.ast.IfStatement
import cnc.ast.NumberLiteral
import cnc.ast.StringLiteral
import cnc.ast.UnaryExpression
import cnc.common.ConfigReader

private object FormatConfigKeys {
    val equalsNoSpace = listOf("enforce-no-spacing-around-equals", "no-space-around-equals")
    val equalsSpace = listOf("enforce-spacing-around-equals", "space-around-equals")
    val colonBefore = listOf("enforce-spacing-before-colon-in-declaration", "space-before-colon")
    val colonAfter = listOf("enforce-spacing-after-colon-in-declaration", "space-after-colon")
    val operatorSpace = listOf("mandatory-space-surrounding-operations", "enforce-spacing-around-operations")
    val singleSpaceSeparation = listOf("mandatory-single-space-separation", "single-space-separation")
    val printlnLineBreaks = listOf("line-breaks-after-println", "line-break-after-println")
    val ifBraceBelowLine = listOf("if-brace-below-line")
    val ifBraceSameLine = listOf("if-brace-same-line")
    val indentInsideIf = listOf("indent-inside-if")

    fun resolveAssignSpacing(
        reader: ConfigReader,
        fallback: Spacing,
    ): Spacing =
        when {
            reader.isEnabled(equalsNoSpace) -> Spacing.NONE
            reader.isEnabled(equalsSpace) -> Spacing.AROUND
            else -> fallback
        }
}

private fun formatOperand(
    child: Expression,
    parentPrecedence: Int,
    formatter: Formatter,
    precedences: Map<String, Int>,
): String {
    val rendered = formatter.format(child)
    val binary = child as? BinaryExpression ?: return rendered
    val childPrecedence =
        precedences[binary.operator]
            ?: error("No precedence registered for operator '${binary.operator}'")
    return if (childPrecedence < parentPrecedence) "($rendered)" else rendered
}

private fun formatCall(
    function: String,
    arguments: List<Expression>,
    formatter: Formatter,
    spaceInsideParens: Boolean,
): String {
    val args = arguments.joinToString(", ") { formatter.format(it) }
    return if (spaceInsideParens && arguments.isNotEmpty()) {
        "$function ( $args )"
    } else {
        "$function($args)"
    }
}

/** Formatea literales numéricos omitiendo `.0` en enteros exactos. */
object NumberLiteralRule : ExpressionFormatRule<NumberLiteral> {
    override fun format(
        expression: NumberLiteral,
        formatter: Formatter,
    ): String =
        if (expression.value % 1.0 == 0.0) {
            expression.value.toLong().toString()
        } else {
            expression.value.toString()
        }
}

/** Formatea literales string entre comillas dobles. */
object StringLiteralRule : ExpressionFormatRule<StringLiteral> {
    override fun format(
        expression: StringLiteral,
        formatter: Formatter,
    ): String = "\"${expression.value}\""
}

/** Formatea literales booleanos (`true` / `false`). */
object BooleanLiteralRule : ExpressionFormatRule<BooleanLiteral> {
    override fun format(
        expression: BooleanLiteral,
        formatter: Formatter,
    ): String = expression.value.toString()
}

/** Formatea identificadores por su nombre. */
object IdentifierRule : ExpressionFormatRule<Identifier> {
    override fun format(
        expression: Identifier,
        formatter: Formatter,
    ): String = expression.name
}

/** Formatea expresiones binarias reintroduciendo paréntesis según [precedences]. */
data class BinaryExpressionRule(
    val precedences: Map<String, Int> = StandardFormatRules.defaultPrecedences,
    val operatorSpacing: Spacing = Spacing.AROUND,
) : ExpressionFormatRule<BinaryExpression> {
    override fun format(
        expression: BinaryExpression,
        formatter: Formatter,
    ): String {
        val precedence =
            precedences[expression.operator]
                ?: error("No precedence registered for operator '${expression.operator}'")
        val left = formatOperand(expression.left, precedence, formatter, precedences)
        val right = formatOperand(expression.right, precedence, formatter, precedences)
        return "$left${operatorSpacing.apply(expression.operator)}$right"
    }

    override fun configure(reader: ConfigReader): BinaryExpressionRule =
        if (reader.isEnabled(FormatConfigKeys.operatorSpace)) {
            copy(operatorSpacing = Spacing.AROUND)
        } else {
            this
        }
}

/** Formatea expresiones unarias preservando paréntesis cuando el operando tiene menor precedencia. */
data class UnaryExpressionRule(
    val precedences: Map<String, Int> = StandardFormatRules.defaultPrecedences,
) : ExpressionFormatRule<UnaryExpression> {
    override fun format(
        expression: UnaryExpression,
        formatter: Formatter,
    ): String {
        val precedence = precedences[expression.operator] ?: Int.MAX_VALUE
        val operand = formatOperand(expression.operand, precedence, formatter, precedences)
        return "${expression.operator}$operand"
    }
}

/** Formatea llamadas usadas como expresión (ej. `readInput("...")`). */
data class CallExpressionRule(
    val spaceInsideParens: Boolean = false,
) : ExpressionFormatRule<CallExpression> {
    override fun format(
        expression: CallExpression,
        formatter: Formatter,
    ): String = formatCall(expression.function, expression.arguments, formatter, spaceInsideParens)

    override fun configure(reader: ConfigReader): CallExpressionRule =
        if (reader.isEnabled(FormatConfigKeys.singleSpaceSeparation)) {
            copy(spaceInsideParens = true)
        } else {
            this
        }
}

/** Formatea declaraciones de variables (`let` / `const`). */
data class DeclarationRule(
    val colonSpacing: Spacing = Spacing.AFTER,
    val assignSpacing: Spacing = Spacing.AROUND,
) : StatementFormatRule<Declaration> {
    override fun format(
        statement: Declaration,
        formatter: Formatter,
    ): String =
        buildString {
            append(if (statement.isMutable) "let" else "const")
            append(" ")
            append(statement.name)
            append(colonSpacing.apply(":"))
            append(statement.type)
            statement.value?.let {
                append(assignSpacing.apply("="))
                append(formatter.format(it))
            }
            append(";")
        }

    override fun configure(reader: ConfigReader): DeclarationRule {
        val newColonSpacing = resolveColonSpacing(reader)
        val newAssignSpacing = FormatConfigKeys.resolveAssignSpacing(reader, assignSpacing)
        return if (newColonSpacing == colonSpacing && newAssignSpacing == assignSpacing) {
            this
        } else {
            copy(colonSpacing = newColonSpacing, assignSpacing = newAssignSpacing)
        }
    }

    private fun resolveColonSpacing(reader: ConfigReader): Spacing {
        if (reader.isEnabled(FormatConfigKeys.singleSpaceSeparation)) return Spacing.AROUND
        val spaceBefore = reader.isEnabled(FormatConfigKeys.colonBefore)
        val spaceAfter = reader.isEnabled(FormatConfigKeys.colonAfter)
        return when {
            spaceBefore && spaceAfter -> Spacing.AROUND
            spaceBefore -> Spacing.BEFORE
            spaceAfter -> Spacing.AFTER
            else -> colonSpacing
        }
    }
}

/** Formatea asignaciones a variables existentes. */
data class AssignmentRule(
    val assignSpacing: Spacing = Spacing.AROUND,
) : StatementFormatRule<Assignment> {
    override fun format(
        statement: Assignment,
        formatter: Formatter,
    ): String = "${statement.target}${assignSpacing.apply("=")}${formatter.format(statement.value)};"

    override fun configure(reader: ConfigReader): AssignmentRule {
        val newAssignSpacing = FormatConfigKeys.resolveAssignSpacing(reader, assignSpacing)
        return if (newAssignSpacing == assignSpacing) this else copy(assignSpacing = newAssignSpacing)
    }
}

/** Formatea llamadas a función usadas como sentencia, con soporte de saltos tras `println`. */
data class CallStatementRule(
    val spaceInsideParens: Boolean = false,
    val lineBreaksAfterPrintln: Int = 0,
) : StatementFormatRule<Call> {
    override fun format(
        statement: Call,
        formatter: Formatter,
    ): String {
        val base = formatCall(statement.function, statement.arguments, formatter, spaceInsideParens) + ";"
        return if (statement.function == "println" && lineBreaksAfterPrintln > 0) {
            base + "\n".repeat(lineBreaksAfterPrintln)
        } else {
            base
        }
    }

    override fun configure(reader: ConfigReader): CallStatementRule {
        val newSpace = if (reader.isEnabled(FormatConfigKeys.singleSpaceSeparation)) true else spaceInsideParens
        val newBreaks = reader.readInt(FormatConfigKeys.printlnLineBreaks) ?: lineBreaksAfterPrintln
        return if (newSpace == spaceInsideParens && newBreaks == lineBreaksAfterPrintln) {
            this
        } else {
            copy(spaceInsideParens = newSpace, lineBreaksAfterPrintln = newBreaks)
        }
    }
}

/** Formatea bloques `{ ... }` aplicando [indentSize] espacios por nivel. */
data class BlockStatementRule(
    val indentSize: Int = StandardFormatRules.DEFAULT_INDENT,
) : StatementFormatRule<BlockStatement> {
    override fun format(
        statement: BlockStatement,
        formatter: Formatter,
    ): String {
        if (statement.statements.isEmpty()) return "{}"
        val indent = " ".repeat(indentSize.coerceAtLeast(0))
        val formattedStatements = statement.statements.map { formatter.format(it) }
        return "{\n" + formattedStatements.joinToString("\n") { indent + it.replace("\n", "\n$indent") } + "\n}"
    }

    override fun configure(reader: ConfigReader): BlockStatementRule {
        val indent = reader.readInt(FormatConfigKeys.indentInsideIf) ?: StandardFormatRules.JSON_FALLBACK_INDENT
        return if (indent == indentSize) this else copy(indentSize = indent)
    }
}

/** Formatea sentencias condicionales `if` / `else`. */
data class IfStatementRule(
    val braceBelowLine: Boolean = false,
) : StatementFormatRule<IfStatement> {
    override fun format(
        statement: IfStatement,
        formatter: Formatter,
    ): String {
        val cond = formatter.format(statement.condition)
        val thenBlock = formatter.format(statement.thenBlock)
        val sep = if (braceBelowLine) "\n" else " "
        val elseBlock = statement.elseBlock
        return if (elseBlock != null) {
            "if ($cond)$sep$thenBlock else ${formatter.format(elseBlock)}"
        } else {
            "if ($cond)$sep$thenBlock"
        }
    }

    override fun configure(reader: ConfigReader): IfStatementRule {
        val below =
            when {
                reader.isEnabled(FormatConfigKeys.ifBraceBelowLine) -> true
                reader.isEnabled(FormatConfigKeys.ifBraceSameLine) -> false
                else -> return this
            }
        return if (below == braceBelowLine) this else copy(braceBelowLine = below)
    }
}

/** Catálogo de reglas de formato estándar reutilizables y auto-configurables. */
object StandardFormatRules {
    const val DEFAULT_INDENT = 4
    const val JSON_FALLBACK_INDENT = 2

    val defaultPrecedences: Map<String, Int> =
        mapOf(
            "+" to 1,
            "-" to 1,
            "*" to 2,
            "/" to 2,
            "**" to 3,
        )
}
