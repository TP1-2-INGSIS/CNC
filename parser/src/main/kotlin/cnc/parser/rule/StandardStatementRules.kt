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

    if (!isMutable && initializer == null) {
        throw ParseAbortException(
            cnc.common.Failure("Syntax error: const declaration '${name}' must have an initializer", cnc.common.ErrorType.PARSER)
        )
    }

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

val ifRule: StatementRule<cnc.ast.IfStatement> = statementRule(
    tag = "if",
    canStart = { cursor -> cursor.peek(0)?.text == "if" }
) {
    expect(TokenType.KEYWORD, "if")
    expect(TokenType.SYMBOL, "(")
    val condition = parseExpression()
    expect(TokenType.SYMBOL, ")")

    val thenBlock = parseBlock()

    var elseBlock: cnc.ast.BlockStatement? = null
    if (match(TokenType.KEYWORD, "else")) {
        elseBlock = parseBlock()
    }

    cnc.ast.IfStatement(condition, thenBlock, elseBlock)
}

object StandardStatementRules {
    val declaration = declarationRule
    val assignment = assignmentRule
    val call = callRule
    val ifStatement = ifRule

    @Suppress("UNCHECKED_CAST")
    val printScript10: List<StatementRule<Statement>> = listOf(
        declaration,
        assignment,
        call,
        ifStatement
    ) as List<StatementRule<Statement>>
}
