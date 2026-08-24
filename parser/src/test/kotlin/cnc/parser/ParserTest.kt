package cnc.parser

import cnc.ast.*
import cnc.common.Position
import cnc.token.Token
import cnc.token.TokenType
import cnc.token.TokenDefinition
import cnc.token.TokenDefinitionProvider
import cnc.token.SymbolTokenDef
import cnc.token.RegexTokenDef
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

// ---------------------------------------------------------------------------
// Definitions de prueba — sin depender del módulo :app
// ---------------------------------------------------------------------------
private val TestTermination    = SymbolTokenDef("semicolon", ";")
private val TestAssign         = SymbolTokenDef("assign", "=")
private val TestColon          = SymbolTokenDef("colon", ":")
private val TestLet            = SymbolTokenDef("let", "let")
private val TestPlus           = SymbolTokenDef("plus", "+")
private val TestMinus          = SymbolTokenDef("minus", "-")
private val TestMul            = SymbolTokenDef("mul", "*")
private val TestDiv            = SymbolTokenDef("div", "/")
private val TestPow            = SymbolTokenDef("pow", "**")
private val TestIdentifier     = RegexTokenDef("identifier", "[a-zA-Z_][a-zA-Z0-9_]*")
private val TestNumber         = RegexTokenDef("number", "[0-9]+")
private val TestString         = RegexTokenDef("string", "\".*?\"")
private val TestNumberType     = SymbolTokenDef("number_type", "number")
private val TestStringType     = SymbolTokenDef("string_type", "string")

private object TestTokenDefs : TokenDefinitionProvider {
    private val allDefs = listOf(
        TestTermination, TestAssign, TestColon, TestLet,
        TestIdentifier, TestNumber, TestString, TestNumberType, TestStringType
    )

    private val definitions = mapOf<TokenType, List<TokenDefinition>>(
        TokenType.VARIABLE_TYPE to listOf(TestNumberType, TestStringType)
    )

    override fun getValue(type: TokenType): List<TokenDefinition>? = definitions[type]
    override fun getTypes(): Set<TokenType> = definitions.keys
    override fun getDefinition(alias: String): TokenDefinition = allDefs.first { it.alias == alias }
    override fun type(str: String): TokenType = TokenType.INVALID
}

// ---------------------------------------------------------------------------
// ExpressionBuilder de prueba — con soporte para operadores
// ---------------------------------------------------------------------------
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
    )
)

// ---------------------------------------------------------------------------
// Gramática de prueba: VariableDeclaration
// ---------------------------------------------------------------------------
private val TestVariableDeclaration = Grammar(
    tag = "VariableDeclaration",
    sequence = listOf(
        IsStrat(TestLet),
        IsStrat(TestIdentifier),
        IsStrat(TestColon),
        AnyOfTypeStrat(listOf(TestNumberType, TestStringType)),
        IsStrat(TestAssign),
        ExpressionStrat(listOf(TestNumber, TestString, TestIdentifier, TestPlus, TestMinus, TestMul, TestDiv, TestPow)),
        IsStrat(TestTermination)
    ),
    build = { segments ->
        Declaration(
            name = segments[1].first().text,
            type = segments[3].first().text,
            value = testExprBuilder.build(segments[5])
        )
    }
)

private val testGrammars = listOf(TestVariableDeclaration)

// ---------------------------------------------------------------------------
// Helpers
// ---------------------------------------------------------------------------
private fun tok(type: TokenType, text: String) = Token(type, Position(0, 0), text)

// ---------------------------------------------------------------------------
// Suite principal
// ---------------------------------------------------------------------------
class ParserTest {

    private val parser = Parser(testGrammars, listOf(TestTermination))

    // -------------------------------------------------------------------------
    // Declaraciones de variable
    // -------------------------------------------------------------------------

    @Nested
    inner class VariableDeclarations {

        @Test fun `declaracion de numero`() {
            val tokens = sequenceOf(
                tok(TokenType.KEYWORD, "let"),
                tok(TokenType.IDENTIFIER, "x"),
                tok(TokenType.SYMBOL, ":"),
                tok(TokenType.VARIABLE_TYPE, "number"),
                tok(TokenType.SYMBOL, "="),
                tok(TokenType.NUMBER, "42"),
                tok(TokenType.SYMBOL, ";")
            )
            val result = parser.getASTs(tokens).toList()
            assertEquals(1, result.size)
            val decl = result[0] as Declaration
            assertEquals("x", decl.name)
            assertEquals("number", decl.type)
            assertEquals(NumberLiteral(42.0), decl.value)
        }

        @Test fun `declaracion de string`() {
            val tokens = sequenceOf(
                tok(TokenType.KEYWORD, "let"),
                tok(TokenType.IDENTIFIER, "name"),
                tok(TokenType.SYMBOL, ":"),
                tok(TokenType.VARIABLE_TYPE, "string"),
                tok(TokenType.SYMBOL, "="),
                tok(TokenType.STRING, "\"hello\""),
                tok(TokenType.SYMBOL, ";")
            )
            val result = parser.getASTs(tokens).toList()
            assertEquals(1, result.size)
            val decl = result[0] as Declaration
            assertEquals("name", decl.name)
            assertEquals("string", decl.type)
            assertEquals(StringLiteral("hello"), decl.value)
        }

        @Test fun `multiples declaraciones`() {
            val tokens = sequenceOf(
                tok(TokenType.KEYWORD, "let"),
                tok(TokenType.IDENTIFIER, "a"),
                tok(TokenType.SYMBOL, ":"),
                tok(TokenType.VARIABLE_TYPE, "number"),
                tok(TokenType.SYMBOL, "="),
                tok(TokenType.NUMBER, "1"),
                tok(TokenType.SYMBOL, ";"),
                tok(TokenType.KEYWORD, "let"),
                tok(TokenType.IDENTIFIER, "b"),
                tok(TokenType.SYMBOL, ":"),
                tok(TokenType.VARIABLE_TYPE, "number"),
                tok(TokenType.SYMBOL, "="),
                tok(TokenType.NUMBER, "2"),
                tok(TokenType.SYMBOL, ";")
            )
            val result = parser.getASTs(tokens).toList()
            assertEquals(2, result.size)
            assertEquals("a", (result[0] as Declaration).name)
            assertEquals("b", (result[1] as Declaration).name)
        }
    }

    // -------------------------------------------------------------------------
    // Errores
    // -------------------------------------------------------------------------

    @Nested
    inner class Errors {

        @Test fun `tokens sin gramatica valida lanza error`() {
            val tokens = sequenceOf(
                tok(TokenType.IDENTIFIER, "x"),
                tok(TokenType.SYMBOL, ";")
            )
            assertThrows<IllegalStateException> {
                parser.getASTs(tokens).toList()
            }
        }

        @Test fun `tokens incompletos lanza error`() {
            val tokens = sequenceOf(
                tok(TokenType.KEYWORD, "let"),
                tok(TokenType.IDENTIFIER, "x"),
                tok(TokenType.SYMBOL, ";")
            )
            assertThrows<IllegalStateException> {
                parser.getASTs(tokens).toList()
            }
        }
    }

    // -------------------------------------------------------------------------
    // splitAfter
    // -------------------------------------------------------------------------

    @Nested
    inner class SplitAfterTests {

        @Test fun `split por terminador agrupa correctamente`() {
            val tokens = sequenceOf(
                tok(TokenType.KEYWORD, "let"),
                tok(TokenType.SYMBOL, ";"),
                tok(TokenType.KEYWORD, "let"),
                tok(TokenType.SYMBOL, ";")
            )
            val groups = tokens.splitAfter { it.text == ";" }.toList()
            assertEquals(2, groups.size)
            assertEquals(2, groups[0].size)
            assertEquals(2, groups[1].size)
        }

        @Test fun `tokens sin terminador quedan en un solo grupo`() {
            val tokens = sequenceOf(
                tok(TokenType.KEYWORD, "let"),
                tok(TokenType.IDENTIFIER, "x")
            )
            val groups = tokens.splitAfter { it.text == ";" }.toList()
            assertEquals(1, groups.size)
            assertEquals(2, groups[0].size)
        }

        @Test fun `secuencia vacia produce cero grupos`() {
            val groups = emptySequence<Token>().splitAfter { it.text == ";" }.toList()
            assertEquals(0, groups.size)
        }
    }

    // -------------------------------------------------------------------------
    // Expresiones con precedencia (Pratt parser)
    // -------------------------------------------------------------------------

    @Nested
    inner class ExpressionPrecedence {

        @Test fun `literal simple`() {
            val tokens = listOf(tok(TokenType.NUMBER, "5"))
            val expr = testExprBuilder.build(tokens)
            assertEquals(NumberLiteral(5.0), expr)
        }

        @Test fun `suma simple`() {
            // 2 + 3 → BinaryExpression(2, "+", 3)
            val tokens = listOf(
                tok(TokenType.NUMBER, "2"),
                tok(TokenType.OPERATOR, "+"),
                tok(TokenType.NUMBER, "3")
            )
            val expr = testExprBuilder.build(tokens)
            assertEquals(
                BinaryExpression(NumberLiteral(2.0), "+", NumberLiteral(3.0)),
                expr
            )
        }

        @Test fun `multiplicacion tiene mayor precedencia que suma`() {
            // 2 + 3 * 4 → BinaryExpression(2, "+", BinaryExpression(3, "*", 4))
            val tokens = listOf(
                tok(TokenType.NUMBER, "2"),
                tok(TokenType.OPERATOR, "+"),
                tok(TokenType.NUMBER, "3"),
                tok(TokenType.OPERATOR, "*"),
                tok(TokenType.NUMBER, "4")
            )
            val expr = testExprBuilder.build(tokens)
            assertEquals(
                BinaryExpression(
                    NumberLiteral(2.0),
                    "+",
                    BinaryExpression(NumberLiteral(3.0), "*", NumberLiteral(4.0))
                ),
                expr
            )
        }

        @Test fun `suma es asociativa a izquierda`() {
            // 1 + 2 + 3 → BinaryExpression(BinaryExpression(1, "+", 2), "+", 3)
            val tokens = listOf(
                tok(TokenType.NUMBER, "1"),
                tok(TokenType.OPERATOR, "+"),
                tok(TokenType.NUMBER, "2"),
                tok(TokenType.OPERATOR, "+"),
                tok(TokenType.NUMBER, "3")
            )
            val expr = testExprBuilder.build(tokens)
            assertEquals(
                BinaryExpression(
                    BinaryExpression(NumberLiteral(1.0), "+", NumberLiteral(2.0)),
                    "+",
                    NumberLiteral(3.0)
                ),
                expr
            )
        }

        @Test fun `exponente es asociativo a derecha`() {
            // 2 ** 3 ** 4 → BinaryExpression(2, "**", BinaryExpression(3, "**", 4))
            val tokens = listOf(
                tok(TokenType.NUMBER, "2"),
                tok(TokenType.OPERATOR, "**"),
                tok(TokenType.NUMBER, "3"),
                tok(TokenType.OPERATOR, "**"),
                tok(TokenType.NUMBER, "4")
            )
            val expr = testExprBuilder.build(tokens)
            assertEquals(
                BinaryExpression(
                    NumberLiteral(2.0),
                    "**",
                    BinaryExpression(NumberLiteral(3.0), "**", NumberLiteral(4.0))
                ),
                expr
            )
        }

        @Test fun `expresion compleja con multiples niveles de precedencia`() {
            // 1 + 2 * 3 + 4 → ((1 + (2 * 3)) + 4)
            val tokens = listOf(
                tok(TokenType.NUMBER, "1"),
                tok(TokenType.OPERATOR, "+"),
                tok(TokenType.NUMBER, "2"),
                tok(TokenType.OPERATOR, "*"),
                tok(TokenType.NUMBER, "3"),
                tok(TokenType.OPERATOR, "+"),
                tok(TokenType.NUMBER, "4")
            )
            val expr = testExprBuilder.build(tokens)
            assertEquals(
                BinaryExpression(
                    BinaryExpression(
                        NumberLiteral(1.0),
                        "+",
                        BinaryExpression(NumberLiteral(2.0), "*", NumberLiteral(3.0))
                    ),
                    "+",
                    NumberLiteral(4.0)
                ),
                expr
            )
        }

        @Test fun `expresion con identificadores`() {
            // x + y * z → BinaryExpression(x, "+", BinaryExpression(y, "*", z))
            val tokens = listOf(
                tok(TokenType.IDENTIFIER, "x"),
                tok(TokenType.OPERATOR, "+"),
                tok(TokenType.IDENTIFIER, "y"),
                tok(TokenType.OPERATOR, "*"),
                tok(TokenType.IDENTIFIER, "z")
            )
            val expr = testExprBuilder.build(tokens)
            assertEquals(
                BinaryExpression(
                    Identifier("x"),
                    "+",
                    BinaryExpression(Identifier("y"), "*", Identifier("z"))
                ),
                expr
            )
        }

        @Test fun `declaracion con expresion binaria`() {
            // let x: number = 2 + 3;
            val tokens = sequenceOf(
                tok(TokenType.KEYWORD, "let"),
                tok(TokenType.IDENTIFIER, "x"),
                tok(TokenType.SYMBOL, ":"),
                tok(TokenType.VARIABLE_TYPE, "number"),
                tok(TokenType.SYMBOL, "="),
                tok(TokenType.NUMBER, "2"),
                tok(TokenType.OPERATOR, "+"),
                tok(TokenType.NUMBER, "3"),
                tok(TokenType.SYMBOL, ";")
            )
            val result = parser.getASTs(tokens).toList()
            assertEquals(1, result.size)
            val decl = result[0] as Declaration
            assertEquals("x", decl.name)
            assertEquals(
                BinaryExpression(NumberLiteral(2.0), "+", NumberLiteral(3.0)),
                decl.value
            )
        }
    }
}
