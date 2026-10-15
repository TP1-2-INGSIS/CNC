package cnc.parser

import cnc.ast.BinaryExpression
import cnc.ast.Call
import cnc.ast.Identifier
import cnc.ast.NumberLiteral
import cnc.ast.Statement
import cnc.common.Failure
import cnc.common.Position
import cnc.common.Success
import cnc.common.asCursor
import cnc.parser.cursor.flow
import cnc.parser.cursor.matches
import cnc.parser.expression.Associativity
import cnc.parser.expression.ExpressionBuilder
import cnc.parser.expression.OperatorDef
import cnc.parser.expression.PrefixOperatorDef
import cnc.parser.flow.FailedFlow
import cnc.parser.flow.OkFlow
import cnc.parser.rule.StatementRule
import cnc.parser.rule.StatementRuleBuilder
import cnc.token.CncKeywords
import cnc.token.CncSymbols
import cnc.token.SymbolTokenDef
import cnc.token.Token
import cnc.token.TokenType
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

class ParserCoverageTest {
    private fun tok(
        text: String,
        type: TokenType = TokenType.SYMBOL,
        row: Int = 1,
        col: Int = 0,
    ): Token = Token(type, Position(row, col), text)

    @Nested
    inner class CursorExtensionsTests {
        @Test
        fun `matches collection and single def with lookahead`() {
            val emptyCursor = emptyList<Token>().asCursor()
            assertFalse(emptyCursor.matches(CncKeywords.LET))
            assertFalse(emptyCursor.matches(TokenType.KEYWORD))
            assertFalse(emptyCursor.matches(listOf(CncKeywords.LET)))
            assertFalse(emptyCursor.matches(TokenType.KEYWORD, listOf(CncKeywords.LET)))
            assertFalse(emptyCursor.matches(TokenType.KEYWORD, CncKeywords.LET))
            assertFalse(emptyCursor.matches(CncKeywords.LET, CncKeywords.CONST))

            val tokens =
                listOf(
                    tok("let", TokenType.KEYWORD),
                    tok("x", TokenType.IDENTIFIER),
                ).asCursor()

            assertTrue(tokens.matches(TokenType.KEYWORD))
            assertTrue(tokens.matches(CncKeywords.LET))
            assertTrue(tokens.matches(listOf(CncKeywords.LET, CncKeywords.CONST)))
            assertTrue(tokens.matches(TokenType.KEYWORD, listOf(CncKeywords.LET)))
            assertTrue(tokens.matches(TokenType.KEYWORD, SymbolTokenDef("id", "x")))
            assertTrue(tokens.matches(CncKeywords.LET, SymbolTokenDef("id", "x")))

            // Mismatches
            assertFalse(tokens.matches(TokenType.IDENTIFIER))
            assertFalse(tokens.matches(CncKeywords.CONST))
            assertFalse(tokens.matches(TokenType.KEYWORD, listOf(CncKeywords.CONST)))
            assertFalse(tokens.matches(TokenType.IDENTIFIER, SymbolTokenDef("id", "x")))
            assertFalse(tokens.matches(CncKeywords.CONST, SymbolTokenDef("id", "x")))
            assertFalse(tokens.matches(CncKeywords.LET, SymbolTokenDef("y", "y")))
        }

        @Test
        fun `matches with only one token returns false for LL2 checks`() {
            val single = listOf(tok("let", TokenType.KEYWORD)).asCursor()
            assertFalse(single.matches(TokenType.KEYWORD, SymbolTokenDef("id", "x")))
            assertFalse(single.matches(CncKeywords.LET, SymbolTokenDef("id", "x")))
        }
    }

    @Nested
    inner class ParseFlowEdgeCasesTests {
        @Test
        fun `OkFlow expect and capture error paths on EOF and mismatches`() {
            val empty = emptyList<Token>().asCursor()
            val flow = empty.flow()

            assertTrue(flow.expect(CncKeywords.LET) is FailedFlow)
            assertTrue(flow.expect(TokenType.KEYWORD, "let") is FailedFlow)
            assertTrue(flow.expect(TokenType.KEYWORD) is FailedFlow)
            assertTrue(flow.expectAny(CncKeywords.LET, CncKeywords.CONST) is FailedFlow)

            assertTrue(flow.capture(CncKeywords.LET) {} is FailedFlow)
            assertTrue(flow.capture(TokenType.KEYWORD) {} is FailedFlow)
            assertTrue(flow.capture(TokenType.KEYWORD, "let") {} is FailedFlow)
            assertTrue(flow.captureAny(listOf(CncKeywords.LET)) {} is FailedFlow)
            assertTrue(flow.captureAny(CncKeywords.LET, CncKeywords.CONST) {} is FailedFlow)

            val wrongToken = listOf(tok("42", TokenType.NUMBER)).asCursor()
            val wrongFlow = wrongToken.flow()

            assertTrue(wrongFlow.expect(CncKeywords.LET) is FailedFlow)
            assertTrue(wrongFlow.expect(TokenType.KEYWORD) is FailedFlow)
            assertTrue(wrongFlow.expect(TokenType.KEYWORD, "let") is FailedFlow)
            assertTrue(wrongFlow.capture(CncKeywords.LET) {} is FailedFlow)
            assertTrue(wrongFlow.capture(TokenType.KEYWORD, "let") {} is FailedFlow)
            assertTrue(wrongFlow.captureAny(listOf(CncKeywords.LET)) {} is FailedFlow)
        }

        @Test
        fun `OkFlow optional and optionalStep behavior`() {
            var captured: Token? = null
            val cursor1 = listOf(tok("let", TokenType.KEYWORD)).asCursor()
            val flow1 = cursor1.flow().optional(TokenType.KEYWORD) { captured = it }
            assertTrue(flow1 is OkFlow)
            assertNotNull(captured)

            var optStepData: String? = null
            val cursor2 = listOf(tok("=", TokenType.SYMBOL), tok("42", TokenType.NUMBER)).asCursor()
            val flow2 =
                cursor2.flow().optionalStep(
                    trigger = CncSymbols.ASSIGN,
                    operation = { c -> Success("ok", c.advance()!!.text) },
                    action = { optStepData = it },
                )
            assertTrue(flow2 is OkFlow)
            assertEquals("42", optStepData)

            // optionalStep trigger mismatch is a no-op success
            val cursor3 = listOf(tok(";", TokenType.SYMBOL)).asCursor()
            val flow3 =
                cursor3.flow().optionalStep(
                    trigger = CncSymbols.ASSIGN,
                    operation = { Success("ok", "never") },
                    action = {},
                )
            assertTrue(flow3 is OkFlow)

            // optionalStep operation failure transitions to FailedFlow
            val cursor4 = listOf(tok("=", TokenType.SYMBOL)).asCursor()
            val flow4 =
                cursor4.flow().optionalStep(
                    trigger = CncSymbols.ASSIGN,
                    operation = { Failure<String>("Op failed", cnc.common.ErrorType.PARSER) },
                    action = {},
                )
            assertTrue(flow4 is FailedFlow)

            // optionalStep with TokenType and text
            val cursor5 = listOf(tok("let", TokenType.KEYWORD), tok("x", TokenType.IDENTIFIER)).asCursor()
            var stepByTokenData: String? = null
            val flow5 =
                cursor5.flow().optionalStep(
                    triggerType = TokenType.KEYWORD,
                    triggerText = "let",
                    operation = { c -> Success("ok", c.advance()!!.text) },
                    action = { stepByTokenData = it },
                )
            assertTrue(flow5 is OkFlow)
            assertEquals("x", stepByTokenData)

            // optionalStep with TokenType mismatch
            val flow6 =
                listOf(tok(";", TokenType.SYMBOL)).asCursor().flow().optionalStep(
                    triggerType = TokenType.KEYWORD,
                    triggerText = "let",
                    operation = { Success("ok", "") },
                    action = {},
                )
            assertTrue(flow6 is OkFlow)
        }

        @Test
        fun `FailedFlow all methods are no-ops and preserve or catch failure`() {
            val cursor = emptyList<Token>().asCursor()
            val initialFailure = Failure<Nothing>("Initial error", cnc.common.ErrorType.PARSER)
            val failed: FailedFlow = FailedFlow(cursor, initialFailure)

            assertTrue(failed.expect(CncKeywords.LET) === failed)
            assertTrue(failed.expect(TokenType.KEYWORD, "let") === failed)
            assertTrue(failed.expectAny(listOf(CncKeywords.LET)) === failed)
            assertTrue(failed.capture(CncKeywords.LET) {} === failed)
            assertTrue(failed.capture(TokenType.KEYWORD) {} === failed)
            assertTrue(failed.capture(TokenType.KEYWORD, "let") {} === failed)
            assertTrue(failed.captureAny(listOf(CncKeywords.LET)) {} === failed)
            assertTrue(failed.optional(CncKeywords.LET) {} === failed)
            assertTrue(failed.optional(TokenType.KEYWORD) {} === failed)
            assertTrue(failed.step({ Success("ok", 1) }, {}) === failed)
            assertTrue(failed.optionalStep(CncSymbols.ASSIGN, { Success("ok", 1) }, {}) === failed)
            assertTrue(failed.optionalStep(TokenType.SYMBOL, "=", { Success("ok", 1) }, {}) === failed)

            val thenRes = failed.then { 42 }
            assertTrue(thenRes is Failure<*>)
            assertEquals("Initial error", thenRes.msg)

            val thenTaggedRes = failed.then("tag") { 42 }
            assertTrue(thenTaggedRes is Failure<*>)

            val thenResultRes = failed.thenResult { Success("ok", 42) }
            assertTrue(thenResultRes is Failure<*>)

            val caught = failed.catch { Failure("Handled error: ${it.msg}", it.type) }
            assertTrue(caught is FailedFlow)
            assertEquals("Handled error: Initial error", (caught as FailedFlow).failure.msg)
        }
    }

    @Nested
    inner class ExpressionBuilderEdgeCasesTests {
        private val numDef = SymbolTokenDef("num", listOf("1", "2", "3", "4", "5", "10", "42"))
        private val idDef = SymbolTokenDef("id", listOf("x", "y", "foo"))
        private val plusDef = SymbolTokenDef("plus", "+")
        private val powDef = SymbolTokenDef("pow", "**")

        private val builder =
            ExpressionBuilder()
                .addRecipe(numDef) { NumberLiteral(it.text.toDouble()) }
                .addRecipe(idDef) { Identifier(it.text) }
                .addOperator(OperatorDef(plusDef, 1, Associativity.LEFT))
                .addOperator(OperatorDef(powDef, 3, Associativity.RIGHT))
                .addPrefixOperator(PrefixOperatorDef(SymbolTokenDef("minus", "-"), 2))

        @Test
        fun `builder additions and copy methods`() {
            val b2 = builder.addRecipeFirst(SymbolTokenDef("extra", "extra")) { Identifier("extra") }
            assertTrue(b2.recipes.containsKey(SymbolTokenDef("extra", "extra")))

            val b3 = builder.addRecipes(mapOf(SymbolTokenDef("a", "a") to { Identifier("a") }))
            assertTrue(b3.recipes.containsKey(SymbolTokenDef("a", "a")))

            val b4 = builder.addRecipesFirst(mapOf(SymbolTokenDef("b", "b") to { Identifier("b") }))
            assertTrue(b4.recipes.containsKey(SymbolTokenDef("b", "b")))

            val b5 = builder.addOperators(listOf(OperatorDef(SymbolTokenDef("mul", "*"), 2)))
            assertEquals(3, b5.operators.size)

            val copied = builder.copy()
            assertEquals(builder.operators.size, copied.operators.size)
        }

        @Test
        fun `build from empty token list or unexpected tokens after expr`() {
            assertTrue(builder.build(emptyList()) is Failure<*>)

            val tokensWithExtra =
                listOf(
                    tok("1", TokenType.NUMBER),
                    tok("+", TokenType.OPERATOR),
                    tok("2", TokenType.NUMBER),
                    tok("extra", TokenType.IDENTIFIER),
                )
            val res = builder.build(tokensWithExtra)
            assertTrue(res is Failure<*>)
            assertTrue(res.msg.contains("Unexpected token 'extra'"))
        }

        @Test
        fun `build atom failure branches`() {
            // Unclosed grouped expr
            val unclosedGroup =
                listOf(
                    tok("(", TokenType.SYMBOL),
                    tok("1", TokenType.NUMBER),
                ).asCursor()
            val groupRes = builder.parse(unclosedGroup)
            assertTrue(groupRes is Failure<*>)
            assertTrue(groupRes.msg.contains("Expected closing ')'"))

            // Token without matching recipe
            val noRecipe = listOf(tok("unknownSymbol", TokenType.SYMBOL)).asCursor()
            val noRecipeRes = builder.parse(noRecipe)
            assertTrue(noRecipeRes is Failure<*>)
            assertTrue(noRecipeRes.msg.contains("No recipe matches token"))

            // Unclosed call expression
            val unclosedCall =
                listOf(
                    tok("foo", TokenType.IDENTIFIER),
                    tok("(", TokenType.SYMBOL),
                    tok("1", TokenType.NUMBER),
                ).asCursor()
            val callRes = builder.parse(unclosedCall)
            assertTrue(callRes is Failure<*>)
            assertTrue(callRes.msg.contains("Expected closing ')'"))
        }

        @Test
        fun `right associativity operator parsing`() {
            // 2 ** 3 ** 4 -> 2 ** (3 ** 4)
            val tokens =
                listOf(
                    tok("2", TokenType.NUMBER),
                    tok("**", TokenType.OPERATOR),
                    tok("3", TokenType.NUMBER),
                    tok("**", TokenType.OPERATOR),
                    tok("4", TokenType.NUMBER),
                ).asCursor()

            val res = builder.parse(tokens)
            assertTrue(res is Success)
            val bin = (res as Success).data as BinaryExpression
            assertEquals("2.0", ((bin.left as NumberLiteral).value).toString())
            assertEquals("**", bin.operator)
            assertTrue(bin.right is BinaryExpression)
        }
    }

    @Nested
    inner class ParserEdgeCasesTests {
        private val letDef = SymbolTokenDef("let", "let")
        private val semiDef = SymbolTokenDef("semi", ";")
        private val openBrace = SymbolTokenDef("open", "{")
        private val closeBrace = SymbolTokenDef("close", "}")

        private val dummyRule =
            StatementRule
                .builder<Statement>("dummy")
                .canStart { it.peek()?.text == "dummy" }
                .parse { cursor, _ ->
                    cursor.advance()
                    Success("ok", Call("dummy", emptyList()))
                }.build()

        private val exprParser = ExpressionBuilder()

        @Test
        fun `parser constructors and getASTs`() {
            val p1 = Parser(dummyRule, expressionParser = exprParser)
            assertNotNull(p1)

            val p2 = Parser(dummyRule, expressionParser = exprParser, blockOpen = openBrace, blockClose = closeBrace)
            assertNotNull(p2)

            val tokens = sequenceOf(tok("dummy", TokenType.IDENTIFIER))
            val asts = p2.getASTs(tokens).toList()
            assertEquals(1, asts.size)
            assertTrue(asts[0] is Success)

            val copy = p2.copy()
            assertEquals(p2.rules.size, copy.rules.size)
        }

        @Test
        fun `parseBlock edge cases without delimiters or delimiter errors`() {
            val pNoDelims = Parser(listOf(dummyRule), exprParser, blockOpen = null, blockClose = null)
            val failBlock = pNoDelims.parseBlock(emptyList<Token>().asCursor())
            assertTrue(failBlock is Failure<*>)
            assertTrue(failBlock.msg.contains("No block open delimiter configured"))

            val pWithDelims = Parser(listOf(dummyRule), exprParser, blockOpen = openBrace, blockClose = closeBrace)

            // Block missing open brace
            val noOpen = listOf(tok("dummy")).asCursor()
            val resNoOpen = pWithDelims.parseBlock(noOpen)
            assertTrue(resNoOpen is Failure<*>)

            // Block missing close brace (EOF while parsing block)
            val noClose = listOf(tok("{"), tok("dummy")).asCursor()
            val resNoClose = pWithDelims.parseBlock(noClose)
            assertTrue(resNoClose is Failure<*>)

            // Block with statement parse error
            val badStmt = listOf(tok("{"), tok("unexpectedToken"), tok("}")).asCursor()
            val resBadStmt = pWithDelims.parseBlock(badStmt)
            assertTrue(resBadStmt is Failure<*>)
        }

        @Test
        fun `parser fail fast sequence on unexpected token`() {
            val p = Parser(listOf(dummyRule), exprParser)
            val tokens =
                sequenceOf(
                    tok("dummy"),
                    tok("badToken"),
                    tok("dummy"),
                )
            val results = p.parse(tokens).toList()
            assertEquals(2, results.size)
            assertTrue(results[0] is Success)
            assertTrue(results[1] is Failure<*>)
        }
    }

    @Nested
    inner class StatementRuleBuilderTests {
        @Test
        fun `builder validates required fields`() {
            assertThrows(IllegalStateException::class.java) {
                StatementRuleBuilder<Statement>().build()
            }

            assertThrows(IllegalStateException::class.java) {
                StatementRuleBuilder<Statement>().withName("myRule").build()
            }

            assertThrows(IllegalStateException::class.java) {
                StatementRuleBuilder<Statement>()
                    .withName("myRule")
                    .starts { true }
                    .build()
            }

            val valid =
                StatementRule
                    .builder<Statement>("testTag")
                    .starts { true }
                    .parse { _, _ -> Success("ok", Call("test", emptyList())) }
                    .build()

            assertEquals("testTag", valid.tag)
            assertTrue(valid.canStart(emptyList<Token>().asCursor()))
        }
    }
}
