package cnc.formatter

import cnc.ast.Assignment
import cnc.ast.BinaryExpression
import cnc.ast.BlockStatement
import cnc.ast.BooleanLiteral
import cnc.ast.Call
import cnc.ast.CallExpression
import cnc.ast.Declaration
import cnc.ast.Identifier
import cnc.ast.IfStatement
import cnc.ast.NumberLiteral
import cnc.ast.StringLiteral
import cnc.ast.UnaryExpression
import cnc.common.Result

internal object TestFormatterFactory {
    fun baseBuilder(): FormatterBuilder =
        Formatter
            .builder()
            .registerStatement(Declaration::class, DeclarationRule())
            .registerStatement(Assignment::class, AssignmentRule())
            .registerStatement(Call::class, CallStatementRule())
            .registerExpression(NumberLiteral::class, NumberLiteralRule)
            .registerExpression(StringLiteral::class, StringLiteralRule)
            .registerExpression(Identifier::class, IdentifierRule)
            .registerExpression(BinaryExpression::class, BinaryExpressionRule())
            .registerExpression(UnaryExpression::class, UnaryExpressionRule())

    fun fullBuilder(): FormatterBuilder =
        baseBuilder()
            .copy()
            .registerStatement(BlockStatement::class, BlockStatementRule())
            .registerStatement(IfStatement::class, IfStatementRule())
            .registerExpression(BooleanLiteral::class, BooleanLiteralRule)
            .registerExpression(CallExpression::class, CallExpressionRule())

    fun defaultFormatter(): Formatter = fullBuilder().build()

    fun build(jsonString: String?): Result<Formatter> = fullBuilder().build(jsonString)

    fun create(jsonString: String? = null): Formatter = fullBuilder().create(jsonString)
}
