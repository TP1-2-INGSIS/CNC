package cnc.parser

import cnc.ast.Assignment
import cnc.ast.BinaryExpression
import cnc.ast.Call
import cnc.ast.Declaration
import cnc.ast.Identifier
import cnc.ast.NumberLiteral
import cnc.ast.StringLiteral
import cnc.ast.UnaryExpression
import cnc.common.Failure
import cnc.common.Position
import cnc.common.Success
import cnc.parser.expression.Associativity
import cnc.parser.expression.ExpressionBuilder
import cnc.parser.expression.OperatorDef
import cnc.parser.expression.PrefixOperatorDef
import cnc.parser.rule.StandardStatementRules
import cnc.token.RegexTokenDef
import cnc.token.SymbolTokenDef
import cnc.token.Token
import cnc.token.TokenType
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

// ---------------------------------------------------------------------------
// Definitions de prueba
// ---------------------------------------------------------------------------
private val TestPlus       = SymbolTokenDef("plus", "+")
private val TestMinus      = SymbolTokenDef("minus", "-")
private val TestMul        = SymbolTokenDef("mul", "*")
private val TestDiv        = SymbolTokenDef("div", "/")
private val TestPow        = SymbolTokenDef("pow", "**")
private val TestOpenParen  = SymbolTokenDef("open_paren", "(")
private val TestCloseParen = SymbolTokenDef("close_paren", ")")
private val TestIdentifier = RegexTokenDef("identifier", "[a-zA-Z_][a-zA-Z0-9_]*")
private val TestNumber     = RegexTokenDef("number", "[0-9]+")
private val TestString     = RegexTokenDef("string", "\".*?\"")

private val testExprBuilder = ExpressionBuilder(
    recipes = mapOf(
        TestNumber to { token: Token -> NumberLiteral(token.text.toDouble()) },
        TestString to { token: Token -> StringLiteral(token.text.removeSurrounding("\"")) },
        TestIdentifier to { token: Token -> Identifier(token.text) }
    ),
    operators = listOf(
        OperatorDef(TestPlus, precedence = 1),
        OperatorDef(TestMinus, precedence = 1),
        OperatorDef(TestMul, precedence = 2),
        OperatorDef(TestDiv, precedence = 2),
        OperatorDef(TestPow, precedence = 3, associativity = Associativity.RIGHT)
    ),
    prefixOperators = listOf(
        PrefixOperatorDef(TestMinus, precedence = 4)
    ),
    groupOpen = TestOpenParen,
    groupClose = TestCloseParen
)

private fun tok(type: TokenType, text: String, row: Int = 0, col: Int = 0) =
    Token(type, Position(row, col), text)

class ParserTest {

    private val parser = Parser(StandardStatementRules.printScript10, testExprBuilder)

    @Nested
    inner class DeclarationTests {

        @Test
        fun `declaration with number initialization`() {
            val tokens = sequenceOf(
                tok(TokenType.KEYWORD, "let"),
                tok(TokenType.IDENTIFIER, "x"),
                tok(TokenType.SYMBOL, ":"),
                tok(TokenType.VARIABLE_TYPE, "number"),
                tok(TokenType.SYMBOL, "="),
                tok(TokenType.NUMBER, "42"),
                tok(TokenType.SYMBOL, ";")
            )

            val results = parser.parse(tokens).toList()
            assertEquals(1, results.size)
            assertTrue(results[0] is Success)

            val stmt = (results[0] as Success).data as Declaration
            assertEquals("x", stmt.name)
            assertEquals("number", stmt.type)
            assertEquals(NumberLiteral(42.0), stmt.value)
            assertTrue(stmt.isMutable)
        }

        @Test
        fun `declaration with string initialization`() {
            val tokens = sequenceOf(
                tok(TokenType.KEYWORD, "const"),
                tok(TokenType.IDENTIFIER, "greeting"),
                tok(TokenType.SYMBOL, ":"),
                tok(TokenType.VARIABLE_TYPE, "string"),
                tok(TokenType.SYMBOL, "="),
                tok(TokenType.STRING, "\"hello\""),
                tok(TokenType.SYMBOL, ";")
            )

            val results = parser.parse(tokens).toList()
            assertEquals(1, results.size)
            val stmt = (results[0] as Success).data as Declaration
            assertEquals("greeting", stmt.name)
            assertEquals("string", stmt.type)
            assertEquals(StringLiteral("hello"), stmt.value)
            assertFalse(stmt.isMutable)
        }

        @Test
        fun `declaration without initialization`() {
            val tokens = sequenceOf(
                tok(TokenType.KEYWORD, "let"),
                tok(TokenType.IDENTIFIER, "x"),
                tok(TokenType.SYMBOL, ":"),
                tok(TokenType.VARIABLE_TYPE, "number"),
                tok(TokenType.SYMBOL, ";")
            )

            val results = parser.parse(tokens).toList()
            assertEquals(1, results.size)
            val stmt = (results[0] as Success).data as Declaration
            assertEquals("x", stmt.name)
            assertEquals("number", stmt.type)
            assertNull(stmt.value)
            assertTrue(stmt.isMutable)
        }

        @Test
        fun `declaration with complex arithmetic expression`() {
            val tokens = sequenceOf(
                tok(TokenType.KEYWORD, "let"),
                tok(TokenType.IDENTIFIER, "res"),
                tok(TokenType.SYMBOL, ":"),
                tok(TokenType.VARIABLE_TYPE, "number"),
                tok(TokenType.SYMBOL, "="),
                tok(TokenType.NUMBER, "2"),
                tok(TokenType.OPERATOR, "+"),
                tok(TokenType.NUMBER, "3"),
                tok(TokenType.OPERATOR, "*"),
                tok(TokenType.NUMBER, "4"),
                tok(TokenType.SYMBOL, ";")
            )

            val results = parser.parse(tokens).toList()
            val stmt = (results[0] as Success).data as Declaration
            val expectedExpr = BinaryExpression(
                left = NumberLiteral(2.0),
                operator = "+",
                right = BinaryExpression(
                    left = NumberLiteral(3.0),
                    operator = "*",
                    right = NumberLiteral(4.0)
                )
            )
            assertEquals(expectedExpr, stmt.value)
        }
    }

    @Nested
    inner class AssignmentTests {

        @Test
        fun `simple variable assignment`() {
            val tokens = sequenceOf(
                tok(TokenType.IDENTIFIER, "x"),
                tok(TokenType.SYMBOL, "="),
                tok(TokenType.NUMBER, "100"),
                tok(TokenType.SYMBOL, ";")
            )

            val results = parser.parse(tokens).toList()
            assertEquals(1, results.size)
            val stmt = (results[0] as Success).data as Assignment
            assertEquals("x", stmt.target)
            assertEquals(NumberLiteral(100.0), stmt.value)
        }
    }

    @Nested
    inner class FunctionCallTests {

        @Test
        fun `function call without arguments`() {
            val tokens = sequenceOf(
                tok(TokenType.IDENTIFIER, "println"),
                tok(TokenType.SYMBOL, "("),
                tok(TokenType.SYMBOL, ")"),
                tok(TokenType.SYMBOL, ";")
            )

            val results = parser.parse(tokens).toList()
            assertEquals(1, results.size)
            val stmt = (results[0] as Success).data as Call
            assertEquals("println", stmt.function)
            assertTrue(stmt.arguments.isEmpty())
        }

        @Test
        fun `function call with multiple comma-separated arguments`() {
            val tokens = sequenceOf(
                tok(TokenType.IDENTIFIER, "print"),
                tok(TokenType.SYMBOL, "("),
                tok(TokenType.STRING, "\"val:\""),
                tok(TokenType.SYMBOL, ","),
                tok(TokenType.NUMBER, "10"),
                tok(TokenType.SYMBOL, ","),
                tok(TokenType.IDENTIFIER, "x"),
                tok(TokenType.SYMBOL, ")"),
                tok(TokenType.SYMBOL, ";")
            )

            val results = parser.parse(tokens).toList()
            assertEquals(1, results.size)
            val stmt = (results[0] as Success).data as Call
            assertEquals("print", stmt.function)
            assertEquals(3, stmt.arguments.size)
            assertEquals(StringLiteral("val:"), stmt.arguments[0])
            assertEquals(NumberLiteral(10.0), stmt.arguments[1])
            assertEquals(Identifier("x"), stmt.arguments[2])
        }
    }

    @Nested
    inner class ContinuousStreamTests {

        @Test
        fun `parses multiple consecutive statements continuously without splitAfter`() {
            val tokens = sequenceOf(
                tok(TokenType.KEYWORD, "let"),
                tok(TokenType.IDENTIFIER, "a"),
                tok(TokenType.SYMBOL, ":"),
                tok(TokenType.VARIABLE_TYPE, "number"),
                tok(TokenType.SYMBOL, "="),
                tok(TokenType.NUMBER, "1"),
                tok(TokenType.SYMBOL, ";"),

                tok(TokenType.IDENTIFIER, "a"),
                tok(TokenType.SYMBOL, "="),
                tok(TokenType.NUMBER, "2"),
                tok(TokenType.SYMBOL, ";"),

                tok(TokenType.IDENTIFIER, "println"),
                tok(TokenType.SYMBOL, "("),
                tok(TokenType.IDENTIFIER, "a"),
                tok(TokenType.SYMBOL, ")"),
                tok(TokenType.SYMBOL, ";")
            )

            val stmts = parser.getASTs(tokens).toList()
            assertEquals(3, stmts.size)
            assertTrue(stmts[0] is Declaration)
            assertTrue(stmts[1] is Assignment)
            assertTrue(stmts[2] is Call)
        }
    }

    @Nested
    inner class SyntaxErrorTests {

        @Test
        fun `missing colon reports syntax error with exact row and col`() {
            val tokens = sequenceOf(
                tok(TokenType.KEYWORD, "let", row = 1, col = 0),
                tok(TokenType.IDENTIFIER, "x", row = 1, col = 4),
                tok(TokenType.SYMBOL, "=", row = 1, col = 6),
                tok(TokenType.NUMBER, "5", row = 1, col = 8),
                tok(TokenType.SYMBOL, ";", row = 1, col = 9)
            )

            val results = parser.parse(tokens).toList()
            assertEquals(1, results.size)
            assertTrue(results[0] is Failure)

            val failure = results[0] as Failure
            assertTrue(failure.msg.contains("row 1, col 6"))
            assertTrue(failure.msg.contains("expected ':'"))
        }

        @Test
        fun `missing semicolon reports syntax error with exact row and col`() {
            val tokens = sequenceOf(
                tok(TokenType.IDENTIFIER, "x", row = 2, col = 0),
                tok(TokenType.SYMBOL, "=", row = 2, col = 2),
                tok(TokenType.NUMBER, "10", row = 2, col = 4)
            )

            val results = parser.parse(tokens).toList()
            assertTrue(results[0] is Failure)
            val failure = results[0] as Failure
            assertTrue(failure.msg.contains("unexpected end of file") || failure.msg.contains("expected ';'"))
        }

        @Test
        fun `unexpected token at statement start reports error and recovers`() {
            val tokens = sequenceOf(
                tok(TokenType.OPERATOR, "+", row = 3, col = 0),
                tok(TokenType.KEYWORD, "let", row = 3, col = 2),
                tok(TokenType.IDENTIFIER, "y", row = 3, col = 6),
                tok(TokenType.SYMBOL, ":", row = 3, col = 7),
                tok(TokenType.VARIABLE_TYPE, "number"),
                tok(TokenType.SYMBOL, ";")
            )

            val results = parser.parse(tokens).toList()
            assertEquals(2, results.size)
            assertTrue(results[0] is Failure)
            assertTrue(results[1] is Success)
        }
    }
}
