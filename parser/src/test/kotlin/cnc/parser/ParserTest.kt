package cnc.parser

import cnc.ast.*
import cnc.common.Position
import cnc.common.Success
import cnc.token.Token
import cnc.token.TokenType
import cnc.token.TokenDefinition
import cnc.token.SymbolTokenDef
import cnc.token.RegexTokenDef
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

// ---------------------------------------------------------------------------
// Definitions de prueba
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
private val TestOpenParen      = SymbolTokenDef("open_paren", "(")
private val TestCloseParen     = SymbolTokenDef("close_paren", ")")
private val TestIdentifier     = RegexTokenDef("identifier", "[a-zA-Z_][a-zA-Z0-9_]*")
private val TestNumber         = RegexTokenDef("number", "[0-9]+")
private val TestString         = RegexTokenDef("string", "\".*?\"")
private val TestNumberType     = SymbolTokenDef("number_type", "number")
private val TestStringType     = SymbolTokenDef("string_type", "string")

// ---------------------------------------------------------------------------
// ExpressionBuilder de prueba
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
    ),
    prefixOperators = listOf(
        PrefixOperatorDef(TestMinus, precedence = 4)
    ),
    groupOpen = TestOpenParen,
    groupClose = TestCloseParen
)

// ---------------------------------------------------------------------------
// StatementDef de prueba
// ---------------------------------------------------------------------------
private val TestDeclarationDef = StatementDef(
    tag = "VariableDeclaration",
    fields = mapOf(
        "name" to FieldType.TEXT,
        "type" to FieldType.TEXT,
        "value" to FieldType.EXPRESSION
    ),
    semanticCheck = { _, _ -> Success("ok", Unit) }
)

// ---------------------------------------------------------------------------
// Gramática de prueba: VariableDeclaration con Steps etiquetados
// ---------------------------------------------------------------------------
private val TestVariableDeclaration = Grammar(
    tag = "VariableDeclaration",
    steps = listOf(
        Step(IsStrat(TestLet)),
        Step(IsStrat(TestIdentifier), label = "name"),
        Step(IsStrat(TestColon)),
        Step(AnyOfTypeStrat(listOf(TestNumberType, TestStringType)), label = "type"),
        Step(IsStrat(TestAssign)),
        Step(ExpressionStrat(listOf(TestNumber, TestString, TestIdentifier, TestPlus, TestMinus, TestMul, TestDiv, TestPow, TestOpenParen, TestCloseParen)), label = "value"),
        Step(IsStrat(TestTermination))
    ),
    statementDef = TestDeclarationDef,
    expressionBuilder = testExprBuilder
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
            val stmt = result[0]
            assertEquals("VariableDeclaration", stmt.tag)
            assertEquals("x", stmt.fields.text("name"))
            assertEquals("number", stmt.fields.text("type"))
            assertEquals(NumberLiteral(42.0), stmt.fields.expression("value"))
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
            val stmt = result[0]
            assertEquals("VariableDeclaration", stmt.tag)
            assertEquals("name", stmt.fields.text("name"))
            assertEquals("string", stmt.fields.text("type"))
            assertEquals(StringLiteral("hello"), stmt.fields.expression("value"))
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
            assertEquals("a", result[0].fields.text("name"))
            assertEquals("b", result[1].fields.text("name"))
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
            val ex = assertThrows<ParseException> {
                parser.getASTs(tokens).toList()
            }
            assertTrue(ex.message!!.contains("Syntax error"))
        }

        @Test fun `tokens incompletos lanza error con contexto de gramatica`() {
            val tokens = sequenceOf(
                tok(TokenType.KEYWORD, "let"),
                tok(TokenType.IDENTIFIER, "x"),
                tok(TokenType.SYMBOL, ";")
            )
            val ex = assertThrows<ParseException> {
                parser.getASTs(tokens).toList()
            }
            assertTrue(ex.message!!.contains("VariableDeclaration"))
            assertTrue(ex.message!!.contains("matched"))
        }

        @Test fun `error incluye posicion del token`() {
            val tokens = sequenceOf(
                Token(TokenType.KEYWORD, Position(3, 5), "let"),
                Token(TokenType.IDENTIFIER, Position(3, 9), "x"),
                Token(TokenType.SYMBOL, Position(3, 10), ";")
            )
            val ex = assertThrows<ParseException> {
                parser.getASTs(tokens).toList()
            }
            assertTrue(ex.message!!.contains("row 3"))
        }

        @Test fun `error indica token encontrado`() {
            val tokens = sequenceOf(
                tok(TokenType.KEYWORD, "let"),
                tok(TokenType.IDENTIFIER, "x"),
                tok(TokenType.SYMBOL, ";")
            )
            val ex = assertThrows<ParseException> {
                parser.getASTs(tokens).toList()
            }
            assertTrue(ex.message!!.contains("';'"))
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
            val stmt = result[0]
            assertEquals("x", stmt.fields.text("name"))
            assertEquals(
                BinaryExpression(NumberLiteral(2.0), "+", NumberLiteral(3.0)),
                stmt.fields.expression("value")
            )
        }
    }

    // -------------------------------------------------------------------------
    // Paréntesis en expresiones
    // -------------------------------------------------------------------------

    @Nested
    inner class Parentheses {

        @Test fun `parentesis simple agrupa subexpresion`() {
            val tokens = listOf(
                tok(TokenType.SYMBOL, "("),
                tok(TokenType.NUMBER, "2"),
                tok(TokenType.OPERATOR, "+"),
                tok(TokenType.NUMBER, "3"),
                tok(TokenType.SYMBOL, ")"),
                tok(TokenType.OPERATOR, "*"),
                tok(TokenType.NUMBER, "4")
            )
            val expr = testExprBuilder.build(tokens)
            assertEquals(
                BinaryExpression(
                    BinaryExpression(NumberLiteral(2.0), "+", NumberLiteral(3.0)),
                    "*",
                    NumberLiteral(4.0)
                ),
                expr
            )
        }

        @Test fun `parentesis anidados`() {
            val tokens = listOf(
                tok(TokenType.SYMBOL, "("),
                tok(TokenType.SYMBOL, "("),
                tok(TokenType.NUMBER, "1"),
                tok(TokenType.OPERATOR, "+"),
                tok(TokenType.NUMBER, "2"),
                tok(TokenType.SYMBOL, ")"),
                tok(TokenType.SYMBOL, ")"),
                tok(TokenType.OPERATOR, "*"),
                tok(TokenType.NUMBER, "3")
            )
            val expr = testExprBuilder.build(tokens)
            assertEquals(
                BinaryExpression(
                    BinaryExpression(NumberLiteral(1.0), "+", NumberLiteral(2.0)),
                    "*",
                    NumberLiteral(3.0)
                ),
                expr
            )
        }

        @Test fun `parentesis a la derecha`() {
            val tokens = listOf(
                tok(TokenType.NUMBER, "4"),
                tok(TokenType.OPERATOR, "*"),
                tok(TokenType.SYMBOL, "("),
                tok(TokenType.NUMBER, "2"),
                tok(TokenType.OPERATOR, "+"),
                tok(TokenType.NUMBER, "3"),
                tok(TokenType.SYMBOL, ")")
            )
            val expr = testExprBuilder.build(tokens)
            assertEquals(
                BinaryExpression(
                    NumberLiteral(4.0),
                    "*",
                    BinaryExpression(NumberLiteral(2.0), "+", NumberLiteral(3.0))
                ),
                expr
            )
        }

        @Test fun `parentesis sin cerrar lanza error`() {
            val tokens = listOf(
                tok(TokenType.SYMBOL, "("),
                tok(TokenType.NUMBER, "2"),
                tok(TokenType.OPERATOR, "+"),
                tok(TokenType.NUMBER, "3")
            )
            assertThrows<IllegalStateException> {
                testExprBuilder.build(tokens)
            }
        }

        @Test fun `parentesis en declaracion`() {
            val tokens = sequenceOf(
                tok(TokenType.KEYWORD, "let"),
                tok(TokenType.IDENTIFIER, "x"),
                tok(TokenType.SYMBOL, ":"),
                tok(TokenType.VARIABLE_TYPE, "number"),
                tok(TokenType.SYMBOL, "="),
                tok(TokenType.SYMBOL, "("),
                tok(TokenType.NUMBER, "2"),
                tok(TokenType.OPERATOR, "+"),
                tok(TokenType.NUMBER, "3"),
                tok(TokenType.SYMBOL, ")"),
                tok(TokenType.OPERATOR, "*"),
                tok(TokenType.NUMBER, "4"),
                tok(TokenType.SYMBOL, ";")
            )
            val result = parser.getASTs(tokens).toList()
            assertEquals(1, result.size)
            val stmt = result[0]
            assertEquals("x", stmt.fields.text("name"))
            assertEquals(
                BinaryExpression(
                    BinaryExpression(NumberLiteral(2.0), "+", NumberLiteral(3.0)),
                    "*",
                    NumberLiteral(4.0)
                ),
                stmt.fields.expression("value")
            )
        }
    }

    // -------------------------------------------------------------------------
    // Operadores unarios (prefix)
    // -------------------------------------------------------------------------

    @Nested
    inner class UnaryOperators {

        @Test fun `negacion de literal`() {
            val tokens = listOf(
                tok(TokenType.OPERATOR, "-"),
                tok(TokenType.NUMBER, "5")
            )
            val expr = testExprBuilder.build(tokens)
            assertEquals(UnaryExpression("-", NumberLiteral(5.0)), expr)
        }

        @Test fun `negacion de identificador`() {
            val tokens = listOf(
                tok(TokenType.OPERATOR, "-"),
                tok(TokenType.IDENTIFIER, "x")
            )
            val expr = testExprBuilder.build(tokens)
            assertEquals(UnaryExpression("-", Identifier("x")), expr)
        }

        @Test fun `negacion tiene mayor precedencia que suma`() {
            val tokens = listOf(
                tok(TokenType.OPERATOR, "-"),
                tok(TokenType.NUMBER, "2"),
                tok(TokenType.OPERATOR, "+"),
                tok(TokenType.NUMBER, "3")
            )
            val expr = testExprBuilder.build(tokens)
            assertEquals(
                BinaryExpression(
                    UnaryExpression("-", NumberLiteral(2.0)),
                    "+",
                    NumberLiteral(3.0)
                ),
                expr
            )
        }

        @Test fun `negacion tiene mayor precedencia que multiplicacion`() {
            val tokens = listOf(
                tok(TokenType.OPERATOR, "-"),
                tok(TokenType.NUMBER, "2"),
                tok(TokenType.OPERATOR, "*"),
                tok(TokenType.NUMBER, "3")
            )
            val expr = testExprBuilder.build(tokens)
            assertEquals(
                BinaryExpression(
                    UnaryExpression("-", NumberLiteral(2.0)),
                    "*",
                    NumberLiteral(3.0)
                ),
                expr
            )
        }

        @Test fun `doble negacion`() {
            val tokens = listOf(
                tok(TokenType.OPERATOR, "-"),
                tok(TokenType.OPERATOR, "-"),
                tok(TokenType.NUMBER, "5")
            )
            val expr = testExprBuilder.build(tokens)
            assertEquals(
                UnaryExpression("-", UnaryExpression("-", NumberLiteral(5.0))),
                expr
            )
        }

        @Test fun `negacion con parentesis`() {
            val tokens = listOf(
                tok(TokenType.OPERATOR, "-"),
                tok(TokenType.SYMBOL, "("),
                tok(TokenType.NUMBER, "2"),
                tok(TokenType.OPERATOR, "+"),
                tok(TokenType.NUMBER, "3"),
                tok(TokenType.SYMBOL, ")")
            )
            val expr = testExprBuilder.build(tokens)
            assertEquals(
                UnaryExpression("-", BinaryExpression(NumberLiteral(2.0), "+", NumberLiteral(3.0))),
                expr
            )
        }
    }
}
