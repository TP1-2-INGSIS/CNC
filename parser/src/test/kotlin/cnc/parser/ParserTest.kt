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
private val TestIdentifier     = RegexTokenDef("identifier", "[a-zA-Z_][a-zA-Z0-9_]*")
private val TestNumber         = RegexTokenDef("number", "[0-9]+")
private val TestString         = RegexTokenDef("string", "\".*?\"")
private val TestNumberType     = SymbolTokenDef("number_type", "number")
private val TestStringType     = SymbolTokenDef("string_type", "string")

private object TestTokenDefs : TokenDefinitionProvider {
    private val definitions = mapOf<TokenType, List<TokenDefinition>>(
        TokenType.VARIABLE_TYPE to listOf(TestNumberType, TestStringType)
    )

    override fun getValue(type: TokenType): List<TokenDefinition>? = definitions[type]
    override fun getTypes(): Set<TokenType> = definitions.keys
    override fun getDefinition(alias: String): TokenDefinition = definitions.values.flatten().first { it.alias == alias }
    override fun type(str: String): TokenType = TokenType.INVALID
}

// ---------------------------------------------------------------------------
// ExpressionBuilder de prueba
// ---------------------------------------------------------------------------
private val testExprBuilder = ExpressionBuilder(mapOf(
    TestNumber to { token: Token -> NumberLiteral(token.text.toDouble()) },
    TestString to { token: Token -> StringLiteral(token.text.removeSurrounding("\"")) },
    TestIdentifier to { token: Token -> Identifier(token.text) }
))

// ---------------------------------------------------------------------------
// Gramática de prueba: VariableDeclaration
// ---------------------------------------------------------------------------
private val TestVariableDeclaration = Grammar(
    tag = "VariableDeclaration",
    sequence = listOf(
        IsStrat(TestLet),
        IsStrat(TestIdentifier),
        IsStrat(TestColon),
        AnyOfTypeStrat(TestTokenDefs.getValue(TokenType.VARIABLE_TYPE)!!),
        IsStrat(TestAssign),
        AnyStrat(listOf(IsStrat(TestNumber), IsStrat(TestString))),
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
}
