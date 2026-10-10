package cnc.parser.rule

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
import cnc.token.CncKeywords
import cnc.token.CncSymbols
import cnc.token.Token
import cnc.token.TokenDefinition
import cnc.token.TokenType

/**
 * Fluent builder for assembling a cohesive, versioned list of [StatementRule]s.
 */
class StatementRulesBuilder {
    private val rules = mutableListOf<StatementRule<Statement>>()

    fun addRule(rule: StatementRule<Statement>): StatementRulesBuilder = apply {
        rules.add(rule)
    }

    fun addDeclaration(vararg keywords: TokenDefinition): StatementRulesBuilder = apply {
        addRule(StandardStatementRules.createDeclarationRule(*keywords))
    }

    fun addAssignment(): StatementRulesBuilder = apply {
        addRule(StandardStatementRules.createAssignmentRule())
    }

    fun addCall(): StatementRulesBuilder = apply {
        addRule(StandardStatementRules.createCallRule())
    }

    fun addIf(): StatementRulesBuilder = apply {
        addRule(StandardStatementRules.createIfRule())
    }

    fun build(): List<StatementRule<Statement>> = rules.toList()
}

/**
 * Factory and presets for standard language statement rules, fully driven by [TokenDefinition]
 * and functional [ParseFlow] without exceptions.
 */
object StandardStatementRules {

    fun builder(): StatementRulesBuilder = StatementRulesBuilder()

    fun createDeclarationRule(vararg keywords: TokenDefinition): StatementRule<Declaration> {
        val keywordList = if (keywords.isEmpty()) listOf(CncKeywords.LET, CncKeywords.CONST) else keywords.toList()

        return StatementRule.builder<Declaration>()
            .withName("declaration")
            .starts { cursor ->
                cursor.matches(TokenType.KEYWORD, keywordList)
            }
            .parse { cursor, expressionParser ->
                var isMutable = true
                var name = ""
                var type = ""
                var initializer: Expression? = null
                var unexpectedKw: Token? = null

                cursor.flow()
                    .capture(TokenType.KEYWORD) { kw ->
                        if (keywordList.none { it.match(kw.text) }) {
                            unexpectedKw = kw
                        }
                        isMutable = CncKeywords.LET.match(kw.text)
                    }
                    .capture(TokenType.IDENTIFIER) { name = it.text }
                    .expect(CncSymbols.COLON)
                    .capture(TokenType.VARIABLE_TYPE) { type = it.text }
                    .optionalStep(CncSymbols.ASSIGN, expressionParser::parse) { initializer = it }
                    .expect(CncSymbols.SEMICOLON)
                    .thenResult {
                        if (unexpectedKw != null) {
                            Failure(
                                "Syntax error at row ${unexpectedKw.pos.row}, col ${unexpectedKw.pos.col}: unexpected keyword '${unexpectedKw.text}'",
                                ErrorType.PARSER
                            )
                        } else if (!isMutable && initializer == null) {
                            Failure("Syntax error: const declaration '$name' must have an initializer", ErrorType.PARSER)
                        } else {
                            Success("Parsed declaration", Declaration(name, type, initializer, isMutable))
                        }
                    }
            }
            .build()
    }

    fun createAssignmentRule(): StatementRule<Assignment> = StatementRule.builder<Assignment>()
        .withName("assignment")
        .starts { cursor ->
            cursor.matches(TokenType.IDENTIFIER, CncSymbols.ASSIGN)
        }
        .parse { cursor, expressionParser ->
            var target = ""
            var value: Expression? = null

            cursor.flow()
                .capture(TokenType.IDENTIFIER) { target = it.text }
                .expect(CncSymbols.ASSIGN)
                .step(expressionParser::parse) { value = it }
                .expect(CncSymbols.SEMICOLON)
                .then("Parsed assignment") {
                    Assignment(target = target, value = value!!)
                }
        }
        .build()

    fun createCallRule(): StatementRule<Call> = StatementRule.builder<Call>()
        .withName("call")
        .starts { cursor ->
            cursor.matches(TokenType.IDENTIFIER, CncSymbols.OPEN_PAREN)
        }
        .parse { cursor, expressionParser ->
            var function = ""
            val arguments = mutableListOf<Expression>()

            val flow = cursor.flow()
                .capture(TokenType.IDENTIFIER) { function = it.text }
                .expect(CncSymbols.OPEN_PAREN)

            if (flow is OkFlow) {
                val next = cursor.peek()
                if (next != null && !CncSymbols.CLOSE_PAREN.match(next.text)) {
                    flow.step(expressionParser::parse) { arguments.add(it) }
                    while (cursor.matches(CncSymbols.COMMA)) {
                        flow.expect(CncSymbols.COMMA)
                        flow.step(expressionParser::parse) { arguments.add(it) }
                    }
                }
            }

            flow.expect(CncSymbols.CLOSE_PAREN)
                .expect(CncSymbols.SEMICOLON)
                .then("Parsed call") {
                    Call(function = function, arguments = arguments)
                }
        }
        .build()

    fun createIfRule(): StatementRule<IfStatement> = StatementRule.builder<IfStatement>()
        .withName("if")
        .starts { cursor ->
            cursor.matches(CncKeywords.IF)
        }
        .parseWithStatements { cursor, expressionParser, nextStatement ->
            var condition: Expression? = null
            var thenBlock: BlockStatement? = null
            var elseBlock: BlockStatement? = null

            val flow = cursor.flow()
                .expect(CncKeywords.IF)
                .expect(CncSymbols.OPEN_PAREN)
                .step(expressionParser::parse) { condition = it }
                .expect(CncSymbols.CLOSE_PAREN)
                .parseBlock(
                    openBrace = CncSymbols.OPEN_BRACE,
                    closeBrace = CncSymbols.CLOSE_BRACE,
                    statementParser = nextStatement
                ) { thenBlock = it }

            if (cursor.matches(CncKeywords.ELSE)) {
                flow.expect(CncKeywords.ELSE)
                    .parseBlock(
                        openBrace = CncSymbols.OPEN_BRACE,
                        closeBrace = CncSymbols.CLOSE_BRACE,
                        statementParser = nextStatement
                    ) { elseBlock = it }
            }

            flow.then("Parsed if") {
                IfStatement(condition!!, thenBlock!!, elseBlock)
            }
        }
        .build()

    val declaration: StatementRule<Declaration> = createDeclarationRule(CncKeywords.LET, CncKeywords.CONST)
    val declarationV10: StatementRule<Declaration> = createDeclarationRule(CncKeywords.LET)
    val assignment: StatementRule<Assignment> = createAssignmentRule()
    val call: StatementRule<Call> = createCallRule()
    val ifStatement: StatementRule<IfStatement> = createIfRule()

    val v1_0: List<StatementRule<Statement>> = builder()
        .addDeclaration(CncKeywords.LET)
        .addAssignment()
        .addCall()
        .build()

    val v1_1: List<StatementRule<Statement>> = builder()
        .addDeclaration(CncKeywords.LET, CncKeywords.CONST)
        .addAssignment()
        .addCall()
        .addIf()
        .build()
}
