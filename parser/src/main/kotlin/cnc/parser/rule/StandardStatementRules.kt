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
import cnc.token.CncKeywords
import cnc.token.CncSymbols
import cnc.token.TokenDefinition
import cnc.token.TokenType

/**
 * Fluent builder for assembling a cohesive, versioned list of [StatementRule]s.
 */
class StatementRulesBuilder {
    private val rules = mutableListOf<StatementRule<Statement>>()

    fun addRule(rule: StatementRule<out Statement>): StatementRulesBuilder = apply {
        @Suppress("UNCHECKED_CAST")
        rules.add(rule as StatementRule<Statement>)
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
 * Factory and presets for standard language statement rules, fully driven by [TokenDefinition].
 */
object StandardStatementRules {

    fun builder(): StatementRulesBuilder = StatementRulesBuilder()

    fun createDeclarationRule(vararg keywords: TokenDefinition): StatementRule<Declaration> {
        val keywordList = if (keywords.isEmpty()) listOf(CncKeywords.LET, CncKeywords.CONST) else keywords.toList()
        return StatementRule.builder<Declaration>("declaration")
            .canStartWith(*keywordList.toTypedArray(), type = TokenType.KEYWORD)
            .parse {
                val kw = expect(TokenType.KEYWORD)
                if (keywordList.none { it.match(kw.text) }) {
                    throw ParseAbortException(
                        Failure("Syntax error at row ${kw.pos.row}, col ${kw.pos.col}: unexpected keyword '${kw.text}'", ErrorType.PARSER)
                    )
                }
                val isMutable = CncKeywords.LET.match(kw.text)
                val name = expect(TokenType.IDENTIFIER).text
                expect(CncSymbols.COLON)
                val type = expect(TokenType.VARIABLE_TYPE).text

                val initializer = if (match(CncSymbols.ASSIGN)) {
                    parseExpression()
                } else null

                if (!isMutable && initializer == null) {
                    throw ParseAbortException(
                        Failure("Syntax error: const declaration '$name' must have an initializer", ErrorType.PARSER)
                    )
                }

                expect(CncSymbols.SEMICOLON)

                Declaration(
                    name = name,
                    type = type,
                    value = initializer,
                    isMutable = isMutable
                )
            }
            .build()
    }

    fun createAssignmentRule(): StatementRule<Assignment> = StatementRule.builder<Assignment>("assignment")
        .canStart { cursor ->
            cursor.peek(0)?.type == TokenType.IDENTIFIER && cursor.peek(1)?.let { CncSymbols.ASSIGN.match(it.text) } == true
        }
        .parse {
            val target = expect(TokenType.IDENTIFIER).text
            expect(CncSymbols.ASSIGN)
            val value = parseExpression()
            expect(CncSymbols.SEMICOLON)

            Assignment(target = target, value = value)
        }
        .build()

    fun createCallRule(): StatementRule<Call> = StatementRule.builder<Call>("call")
        .canStart { cursor ->
            cursor.peek(0)?.type == TokenType.IDENTIFIER && cursor.peek(1)?.let { CncSymbols.OPEN_PAREN.match(it.text) } == true
        }
        .parse {
            val function = expect(TokenType.IDENTIFIER).text
            expect(CncSymbols.OPEN_PAREN)

            val arguments = mutableListOf<Expression>()
            if (peek()?.let { CncSymbols.CLOSE_PAREN.match(it.text) } != true) {
                arguments.add(parseExpression())
                while (match(CncSymbols.COMMA)) {
                    arguments.add(parseExpression())
                }
            }

            expect(CncSymbols.CLOSE_PAREN)
            expect(CncSymbols.SEMICOLON)

            Call(function = function, arguments = arguments)
        }
        .build()

    fun createIfRule(): StatementRule<IfStatement> = StatementRule.builder<IfStatement>("if")
        .canStartWith(CncKeywords.IF, type = TokenType.KEYWORD)
        .parse {
            expect(CncKeywords.IF)
            expect(CncSymbols.OPEN_PAREN)
            val condition = parseExpression()
            expect(CncSymbols.CLOSE_PAREN)

            val thenBlock = parseBlock()

            var elseBlock: BlockStatement? = null
            if (match(CncKeywords.ELSE)) {
                elseBlock = parseBlock()
            }

            IfStatement(condition, thenBlock, elseBlock)
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

    val printScript10: List<StatementRule<Statement>> = v1_1
}
