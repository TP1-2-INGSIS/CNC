package cnc.parser.expression

import cnc.ast.BinaryExpression
import cnc.ast.CallExpression
import cnc.ast.Identifier
import cnc.ast.NumberLiteral
import cnc.ast.StringLiteral
import cnc.ast.UnaryExpression
import cnc.common.Failure
import cnc.common.Position
import cnc.common.Success
import cnc.common.asCursor
import cnc.token.RegexTokenDef
import cnc.token.SymbolTokenDef
import cnc.token.Token
import cnc.token.TokenType
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class ExpressionBuilderTest {
    private val testPlus = SymbolTokenDef("plus", "+")
    private val testMinus = SymbolTokenDef("minus", "-")
    private val testMul = SymbolTokenDef("mul", "*")
    private val testDiv = SymbolTokenDef("div", "/")
    private val testPow = SymbolTokenDef("pow", "**")
    private val testIdentifier = RegexTokenDef("identifier", "[a-zA-Z_][a-zA-Z0-9_]*")
    private val testNumber = RegexTokenDef("number", "[0-9]+")
    private val testString = RegexTokenDef("string", "\".*?\"")

    private val builder =
        ExpressionBuilder(
            recipes =
                mapOf(
                    testNumber to { token -> NumberLiteral(token.text.toDouble()) },
                    testString to { token -> StringLiteral(token.text.removeSurrounding("\"")) },
                    testIdentifier to { token -> Identifier(token.text) },
                ),
            operators =
                listOf(
                    OperatorDef(testPlus, precedence = 1),
                    OperatorDef(testMinus, precedence = 1),
                    OperatorDef(testMul, precedence = 2),
                    OperatorDef(testDiv, precedence = 2),
                    OperatorDef(testPow, precedence = 3, associativity = Associativity.RIGHT),
                ),
            prefixOperators =
                listOf(
                    PrefixOperatorDef(testMinus, precedence = 4),
                ),
        )

    private fun tok(
        type: TokenType,
        text: String,
        row: Int = 1,
        col: Int = 1,
    ) = Token(type, Position(row, col), text)

    @Test
    fun `parses number and string literals and identifiers`() {
        val numRes = builder.parse(sequenceOf(tok(TokenType.NUMBER, "42")).asCursor())
        assertTrue(numRes is Success)
        assertEquals(NumberLiteral(42.0), (numRes as Success).data)

        val strRes = builder.parse(sequenceOf(tok(TokenType.STRING, "\"hello\"")).asCursor())
        assertTrue(strRes is Success)
        assertEquals(StringLiteral("hello"), (strRes as Success).data)

        val idRes = builder.parse(sequenceOf(tok(TokenType.IDENTIFIER, "foo")).asCursor())
        assertTrue(idRes is Success)
        assertEquals(Identifier("foo"), (idRes as Success).data)
    }

    @Test
    fun `respects operator precedence multiplication over addition`() {
        // 1 + 2 * 3
        val tokens =
            sequenceOf(
                tok(TokenType.NUMBER, "1"),
                tok(TokenType.OPERATOR, "+"),
                tok(TokenType.NUMBER, "2"),
                tok(TokenType.OPERATOR, "*"),
                tok(TokenType.NUMBER, "3"),
            )
        val res = builder.parse(tokens.asCursor())
        assertTrue(res is Success)
        val expr = (res as Success).data as BinaryExpression
        assertEquals("+", expr.operator)
        assertEquals(NumberLiteral(1.0), expr.left)
        val right = expr.right as BinaryExpression
        assertEquals("*", right.operator)
        assertEquals(NumberLiteral(2.0), right.left)
        assertEquals(NumberLiteral(3.0), right.right)
    }

    @Test
    fun `respects right associativity for exponentiation`() {
        // 2 ** 3 ** 4  =>  2 ** (3 ** 4)
        val tokens =
            sequenceOf(
                tok(TokenType.NUMBER, "2"),
                tok(TokenType.OPERATOR, "**"),
                tok(TokenType.NUMBER, "3"),
                tok(TokenType.OPERATOR, "**"),
                tok(TokenType.NUMBER, "4"),
            )
        val res = builder.parse(tokens.asCursor())
        assertTrue(res is Success)
        val expr = (res as Success).data as BinaryExpression
        assertEquals("**", expr.operator)
        assertEquals(NumberLiteral(2.0), expr.left)
        val right = expr.right as BinaryExpression
        assertEquals("**", right.operator)
        assertEquals(NumberLiteral(3.0), right.left)
        assertEquals(NumberLiteral(4.0), right.right)
    }

    @Test
    fun `parses grouped expression overriding precedence`() {
        // (1 + 2) * 3
        val tokens =
            sequenceOf(
                tok(TokenType.SYMBOL, "("),
                tok(TokenType.NUMBER, "1"),
                tok(TokenType.OPERATOR, "+"),
                tok(TokenType.NUMBER, "2"),
                tok(TokenType.SYMBOL, ")"),
                tok(TokenType.OPERATOR, "*"),
                tok(TokenType.NUMBER, "3"),
            )
        val res = builder.parse(tokens.asCursor())
        assertTrue(res is Success)
        val expr = (res as Success).data as BinaryExpression
        assertEquals("*", expr.operator)
        val left = expr.left as BinaryExpression
        assertEquals("+", left.operator)
        assertEquals(NumberLiteral(1.0), left.left)
        assertEquals(NumberLiteral(2.0), left.right)
        assertEquals(NumberLiteral(3.0), expr.right)
    }

    @Test
    fun `returns Failure on unclosed grouped expression without throwing exception`() {
        val tokens =
            sequenceOf(
                tok(TokenType.SYMBOL, "("),
                tok(TokenType.NUMBER, "1"),
                tok(TokenType.OPERATOR, "+"),
                tok(TokenType.NUMBER, "2"),
            )
        val res = builder.parse(tokens.asCursor())
        assertTrue(res is Failure)
        val failure = res as Failure
        assertTrue(failure.msg.contains("Expected closing ')'"))
    }

    @Test
    fun `parses unary prefix operators`() {
        // -5
        val tokens =
            sequenceOf(
                tok(TokenType.OPERATOR, "-"),
                tok(TokenType.NUMBER, "5"),
            )
        val res = builder.parse(tokens.asCursor())
        assertTrue(res is Success)
        val expr = (res as Success).data as UnaryExpression
        assertEquals("-", expr.operator)
        assertEquals(NumberLiteral(5.0), expr.operand)
    }

    @Test
    fun `parses function call without arguments`() {
        // foo()
        val tokens =
            sequenceOf(
                tok(TokenType.IDENTIFIER, "foo"),
                tok(TokenType.SYMBOL, "("),
                tok(TokenType.SYMBOL, ")"),
            )
        val res = builder.parse(tokens.asCursor())
        assertTrue(res is Success)
        val expr = (res as Success).data as CallExpression
        assertEquals("foo", expr.function)
        assertTrue(expr.arguments.isEmpty())
    }

    @Test
    fun `parses function call with multiple arguments and in binary expression`() {
        // add(1, 2) + 3
        val tokens =
            sequenceOf(
                tok(TokenType.IDENTIFIER, "add"),
                tok(TokenType.SYMBOL, "("),
                tok(TokenType.NUMBER, "1"),
                tok(TokenType.SYMBOL, ","),
                tok(TokenType.NUMBER, "2"),
                tok(TokenType.SYMBOL, ")"),
                tok(TokenType.OPERATOR, "+"),
                tok(TokenType.NUMBER, "3"),
            )
        val res = builder.parse(tokens.asCursor())
        assertTrue(res is Success)
        val expr = (res as Success).data as BinaryExpression
        assertEquals("+", expr.operator)
        val call = expr.left as CallExpression
        assertEquals("add", call.function)
        assertEquals(2, call.arguments.size)
        assertEquals(NumberLiteral(1.0), call.arguments[0])
        assertEquals(NumberLiteral(2.0), call.arguments[1])
        assertEquals(NumberLiteral(3.0), expr.right)
    }

    @Test
    fun `parses nested function call expressions`() {
        // outer(inner(42))
        val tokens =
            sequenceOf(
                tok(TokenType.IDENTIFIER, "outer"),
                tok(TokenType.SYMBOL, "("),
                tok(TokenType.IDENTIFIER, "inner"),
                tok(TokenType.SYMBOL, "("),
                tok(TokenType.NUMBER, "42"),
                tok(TokenType.SYMBOL, ")"),
                tok(TokenType.SYMBOL, ")"),
            )
        val res = builder.parse(tokens.asCursor())
        assertTrue(res is Success)
        val outer = (res as Success).data as CallExpression
        assertEquals("outer", outer.function)
        assertEquals(1, outer.arguments.size)
        val inner = outer.arguments[0] as CallExpression
        assertEquals("inner", inner.function)
        assertEquals(NumberLiteral(42.0), inner.arguments[0])
    }

    @Test
    fun `returns Failure on unclosed call parentheses`() {
        val tokens =
            sequenceOf(
                tok(TokenType.IDENTIFIER, "foo"),
                tok(TokenType.SYMBOL, "("),
                tok(TokenType.NUMBER, "1"),
            )
        val res = builder.parse(tokens.asCursor())
        assertTrue(res is Failure)
        val failure = res as Failure
        assertTrue(failure.msg.contains("Expected closing ')' after arguments"))
    }

    @Test
    fun `returns Failure on empty tokens or unknown tokens`() {
        val emptyRes = builder.parse(emptySequence<Token>().asCursor())
        assertTrue(emptyRes is Failure)
        assertTrue((emptyRes as Failure).msg.contains("Unexpected end of expression"))

        val unknownRes = builder.parse(sequenceOf(tok(TokenType.SYMBOL, "?")).asCursor())
        assertTrue(unknownRes is Failure)
        assertTrue((unknownRes as Failure).msg.contains("No recipe matches token"))
    }
}
