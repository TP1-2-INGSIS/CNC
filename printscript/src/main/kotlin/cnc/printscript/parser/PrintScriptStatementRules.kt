package cnc.printscript.parser

import cnc.ast.Assignment
import cnc.ast.BlockStatement
import cnc.ast.Call
import cnc.ast.Declaration
import cnc.ast.Expression
import cnc.ast.IfStatement
import cnc.ast.Statement
import cnc.common.ErrorType
import cnc.common.Failure
import cnc.common.Success
import cnc.parser.cursor.flow
import cnc.parser.cursor.matches
import cnc.parser.flow.OkFlow
import cnc.parser.rule.StatementRule
import cnc.token.CncKeywords
import cnc.token.CncSymbols
import cnc.token.TokenDefinition
import cnc.token.TokenType

/**
 * Statement parsing rules specific to the PrintScript language.
 */
object PrintScriptStatementRules {
    fun createDeclarationRule(vararg keywords: TokenDefinition): StatementRule<Declaration> {
        val keywordList = if (keywords.isEmpty()) listOf(CncKeywords.LET, CncKeywords.CONST) else keywords.toList()

        return StatementRule
            .builder<Declaration>()
            .withName("declaration")
            .starts { cursor ->
                cursor.matches(TokenType.KEYWORD, keywordList)
            }.parse { cursor, parser ->
                var isMutable = true
                var name = ""
                var type = ""
                var initializer: Expression? = null

                cursor
                    .flow()
                    .captureAny(keywordList) { kw ->
                        isMutable = CncKeywords.LET.match(kw.text)
                    }.capture(TokenType.IDENTIFIER) { name = it.text }
                    .expect(CncSymbols.COLON)
                    .capture(TokenType.VARIABLE_TYPE) { type = it.text }
                    .optionalStep(CncSymbols.ASSIGN, parser::parseExpression) { initializer = it }
                    .expect(CncSymbols.SEMICOLON)
                    .thenResult {
                        if (!isMutable && initializer == null) {
                            Failure(
                                "Syntax error: const declaration '$name' must have an initializer",
                                ErrorType.PARSER,
                            )
                        } else {
                            Success("Parsed declaration", Declaration(name, type, initializer, isMutable))
                        }
                    }
            }.build()
    }

    fun createAssignmentRule(): StatementRule<Assignment> =
        StatementRule
            .builder<Assignment>()
            .withName("assignment")
            .starts { cursor ->
                cursor.matches(TokenType.IDENTIFIER, CncSymbols.ASSIGN)
            }.parse { cursor, parser ->
                var target = ""
                var value: Expression? = null

                cursor
                    .flow()
                    .capture(TokenType.IDENTIFIER) { target = it.text }
                    .expect(CncSymbols.ASSIGN)
                    .step(parser::parseExpression) { value = it }
                    .expect(CncSymbols.SEMICOLON)
                    .then("Parsed assignment") {
                        Assignment(target = target, value = value!!)
                    }
            }.build()

    fun createCallRule(): StatementRule<Call> =
        StatementRule
            .builder<Call>()
            .withName("call")
            .starts { cursor ->
                cursor.matches(TokenType.IDENTIFIER, CncSymbols.OPEN_PAREN)
            }.parse { cursor, parser ->
                var function = ""
                val arguments = mutableListOf<Expression>()

                val flow =
                    cursor
                        .flow()
                        .capture(TokenType.IDENTIFIER) { function = it.text }
                        .expect(CncSymbols.OPEN_PAREN)

                if (flow is OkFlow) {
                    val next = cursor.peek()
                    if (next != null && !CncSymbols.CLOSE_PAREN.match(next.text)) {
                        flow.step(parser::parseExpression) { arguments.add(it) }
                        while (cursor.matches(CncSymbols.COMMA)) {
                            flow.expect(CncSymbols.COMMA)
                            flow.step(parser::parseExpression) { arguments.add(it) }
                        }
                    }
                }

                flow
                    .expect(CncSymbols.CLOSE_PAREN)
                    .expect(CncSymbols.SEMICOLON)
                    .then("Parsed call") {
                        Call(function = function, arguments = arguments)
                    }
            }.build()

    fun createIfRule(): StatementRule<IfStatement> =
        StatementRule
            .builder<IfStatement>()
            .withName("if")
            .starts { cursor ->
                cursor.matches(CncKeywords.IF)
            }.parse { cursor, parser ->
                var condition: Expression? = null
                var thenBlock: BlockStatement? = null
                var elseBlock: BlockStatement? = null

                val flow =
                    cursor
                        .flow()
                        .expect(CncKeywords.IF)
                        .expect(CncSymbols.OPEN_PAREN)
                        .step(parser::parseExpression) { condition = it }
                        .expect(CncSymbols.CLOSE_PAREN)
                        .step(parser::parseBlock) { thenBlock = it }
                        .optionalStep(CncKeywords.ELSE, parser::parseBlock) { elseBlock = it }

                flow.then("Parsed if") {
                    IfStatement(condition!!, thenBlock!!, elseBlock)
                }
            }.build()

    val v1_0: List<StatementRule<Statement>> =
        listOf(
            createDeclarationRule(CncKeywords.LET),
            createAssignmentRule(),
            createCallRule(),
        )

    val v1_1: List<StatementRule<Statement>> =
        listOf(
            createDeclarationRule(CncKeywords.LET, CncKeywords.CONST),
            createAssignmentRule(),
            createCallRule(),
            createIfRule(),
        )
}
