package cnc.parser.rule

import cnc.ast.Assignment
import cnc.ast.Call
import cnc.ast.Declaration
import cnc.ast.Expression
import cnc.ast.Statement
import cnc.token.TokenType

val declarationRule: StatementRule<Declaration> = statementRule(
    tag = "declaration",
    canStart = { cursor ->
        val first = cursor.peek(0)
        first?.type == TokenType.KEYWORD && first.text in setOf("let", "const")
    }
) {
    val kw = expect(TokenType.KEYWORD)
    val isMutable = kw.text == "let"
    val name = expect(TokenType.IDENTIFIER).text
    expect(TokenType.SYMBOL, ":")
    val type = expect(TokenType.VARIABLE_TYPE).text

    val initializer = if (match(TokenType.SYMBOL, "=")) {
        parseExpression()
    } else null

    expect(TokenType.SYMBOL, ";")

    Declaration(
        name = name,
        type = type,
        value = initializer,
        isMutable = isMutable
    )
}

val assignmentRule: StatementRule<Assignment> = statementRule(
    tag = "assignment",
    canStart = { cursor ->
        cursor.peek(0)?.type == TokenType.IDENTIFIER && cursor.peek(1)?.text == "="
    }
) {
    val target = expect(TokenType.IDENTIFIER).text
    expect(TokenType.SYMBOL, "=")
    val value = parseExpression()
    expect(TokenType.SYMBOL, ";")

    Assignment(target = target, value = value)
}

val callRule: StatementRule<Call> = statementRule(
    tag = "call",
    canStart = { cursor ->
        cursor.peek(0)?.type == TokenType.IDENTIFIER && cursor.peek(1)?.text == "("
    }
) {
    val function = expect(TokenType.IDENTIFIER).text
    expect(TokenType.SYMBOL, "(")

    val arguments = mutableListOf<Expression>()
    if (peek()?.text != ")") {
        arguments.add(parseExpression())
        while (match(TokenType.SYMBOL, ",")) {
            arguments.add(parseExpression())
        }
    }

    expect(TokenType.SYMBOL, ")")
    expect(TokenType.SYMBOL, ";")

    Call(function = function, arguments = arguments)
}

object StandardStatementRules {
    val declaration = declarationRule
    val assignment = assignmentRule
    val call = callRule

    @Suppress("UNCHECKED_CAST")
    val printScript10: List<StatementRule<Statement>> = listOf(
        declaration,
        assignment,
        call
    ) as List<StatementRule<Statement>>
}
