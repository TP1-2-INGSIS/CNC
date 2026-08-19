package cnc.token

import cnc.common.Position
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

// ---------------------------------------------------------------------------
// Concrete TokenDefinitionProvider used across the test suite
// ---------------------------------------------------------------------------
private object TestProvider : TokenDefinitionProvider {

    private val definitions = mapOf(
        TokenType.OPERATOR to listOf(
            SymbolTokenDef(TokenType.OPERATOR, listOf("+", "-", "*", "/", "==")),
        ),
        TokenType.SYMBOL to listOf(
            SymbolTokenDef(TokenType.SYMBOL, listOf(";", ":", "=")),
        ),
        TokenType.KEYWORD to listOf(
            SymbolTokenDef(TokenType.KEYWORD, "let"),
        ),
        TokenType.VARIABLE_TYPE to listOf(
            SymbolTokenDef(TokenType.VARIABLE_TYPE, listOf("string", "number")),
        ),
        TokenType.IDENTIFIER to listOf(
            RegexTokenDef(TokenType.IDENTIFIER, "[a-zA-Z_][a-zA-Z0-9_]*"),
        ),
        TokenType.NUMBER to listOf(
            RegexTokenDef(TokenType.NUMBER, "[0-9]+"),
        ),
        TokenType.STRING to listOf(
            RegexTokenDef(TokenType.STRING, "\".*?\""),
        ),
    )

    override fun getValue(type: TokenType): List<TokenDefinition>? = definitions[type]
    override fun getTypes(): Set<TokenType> = definitions.keys

    override fun type(str: String): TokenType {
        for (type in getTypes()) {
            for (def in definitions[type]!!) {
                if (def.match(str)) return type
            }
        }
        return TokenType.INVALID
    }
}

// ---------------------------------------------------------------------------
// Test suite
// ---------------------------------------------------------------------------
class TokenTest {

    // -----------------------------------------------------------------------
    // Token data class
    // -----------------------------------------------------------------------
    @Nested
    inner class TokenDataClass {

        @Test
        fun `Token stores type, position and text`() {
            val pos = Position(1, 0)
            val token = Token(TokenType.NUMBER, pos, "42")

            assertEquals(TokenType.NUMBER, token.type)
            assertEquals(pos, token.pos)
            assertEquals("42", token.text)
        }

        @Test
        fun `Tokens with same fields are equal`() {
            val pos = Position(3, 5)
            val t1 = Token(TokenType.KEYWORD, pos, "let")
            val t2 = Token(TokenType.KEYWORD, pos, "let")

            assertEquals(t1, t2)
        }

        @Test
        fun `Tokens with different text are not equal`() {
            val pos = Position(0, 0)
            val t1 = Token(TokenType.IDENTIFIER, pos, "foo")
            val t2 = Token(TokenType.IDENTIFIER, pos, "bar")

            assertTrue(t1 != t2)
        }

        @Test
        fun `Tokens with different types are not equal`() {
            val pos = Position(0, 0)
            val t1 = Token(TokenType.KEYWORD, pos, "let")
            val t2 = Token(TokenType.IDENTIFIER, pos, "let")

            assertTrue(t1 != t2)
        }

        @Test
        fun `copy() produces a distinct but equal token`() {
            val original = Token(TokenType.STRING, Position(2, 4), "\"hello\"")
            val copy = original.copy()

            assertEquals(original, copy)
            assertTrue(original !== copy)
        }
    }

    // -----------------------------------------------------------------------
    // SymbolTokenDef
    // -----------------------------------------------------------------------
    @Nested
    inner class SymbolTokenDefTests {

        @Test
        fun `match returns true for a symbol in the list`() {
            val def = SymbolTokenDef(TokenType.OPERATOR, listOf("+", "-", "*"))

            assertTrue(def.match("+"))
            assertTrue(def.match("-"))
            assertTrue(def.match("*"))
        }

        @Test
        fun `match returns false for a symbol not in the list`() {
            val def = SymbolTokenDef(TokenType.OPERATOR, listOf("+", "-"))

            assertFalse(def.match("/"))
            assertFalse(def.match(""))
        }

        @Test
        fun `single-symbol convenience constructor works`() {
            val def = SymbolTokenDef(TokenType.KEYWORD, "let")

            assertTrue(def.match("let"))
            assertFalse(def.match("const"))
            assertEquals(listOf("let"), def.symbols)
        }

        @Test
        fun `type property is preserved`() {
            val def = SymbolTokenDef(TokenType.SYMBOL, listOf(";", ":"))

            assertEquals(TokenType.SYMBOL, def.type)
        }

        @Test
        fun `symbols list is accessible`() {
            val symbols = listOf(";", ":", "=")
            val def = SymbolTokenDef(TokenType.SYMBOL, symbols)

            assertEquals(symbols, def.symbols)
        }
    }

    // -----------------------------------------------------------------------
    // RegexTokenDef
    // -----------------------------------------------------------------------
    @Nested
    inner class RegexTokenDefTests {

        @Test
        fun `match returns true for a valid integer`() {
            val def = RegexTokenDef(TokenType.NUMBER, "[0-9]+")

            assertTrue(def.match("0"))
            assertTrue(def.match("42"))
            assertTrue(def.match("9999"))
        }

        @Test
        fun `match returns false for a non-numeric string`() {
            val def = RegexTokenDef(TokenType.NUMBER, "[0-9]+")

            assertFalse(def.match("abc"))
            assertFalse(def.match(""))
            assertFalse(def.match("1a"))
        }

        @Test
        fun `match works for identifier pattern`() {
            val def = RegexTokenDef(TokenType.IDENTIFIER, "[a-zA-Z_][a-zA-Z0-9_]*")

            assertTrue(def.match("myVar"))
            assertTrue(def.match("_private"))
            assertTrue(def.match("x"))
            assertFalse(def.match("1invalid"))
            assertFalse(def.match(""))
        }

        @Test
        fun `match works for quoted string pattern`() {
            val def = RegexTokenDef(TokenType.STRING, "\".*?\"")

            assertTrue(def.match("\"hello world\""))
            assertTrue(def.match("\"\""))
            assertFalse(def.match("hello"))
            assertFalse(def.match("\"unclosed"))
        }

        @Test
        fun `symbols list contains the regex pattern`() {
            val pattern = "[0-9]+"
            val def = RegexTokenDef(TokenType.NUMBER, pattern)

            assertEquals(listOf(pattern), def.symbols)
        }

        @Test
        fun `type property is preserved`() {
            val def = RegexTokenDef(TokenType.IDENTIFIER, "[a-zA-Z_][a-zA-Z0-9_]*")

            assertEquals(TokenType.IDENTIFIER, def.type)
        }
    }

    // -----------------------------------------------------------------------
    // TokenDefinitionProvider
    // -----------------------------------------------------------------------
    @Nested
    inner class TokenDefinitionProviderTests {

        @Test
        fun `getTypes returns all registered token types`() {
            val types = TestProvider.getTypes()

            assertTrue(types.contains(TokenType.OPERATOR))
            assertTrue(types.contains(TokenType.SYMBOL))
            assertTrue(types.contains(TokenType.KEYWORD))
            assertTrue(types.contains(TokenType.IDENTIFIER))
            assertTrue(types.contains(TokenType.NUMBER))
            assertTrue(types.contains(TokenType.STRING))
            assertTrue(types.contains(TokenType.VARIABLE_TYPE))
        }

        @Test
        fun `getValue returns definitions for a registered type`() {
            val defs = TestProvider.getValue(TokenType.OPERATOR)

            assertTrue(defs != null && defs.isNotEmpty())
        }

        @Test
        fun `getValue returns null for an unregistered type`() {
            // INVALID is not registered in TestProvider
            assertNull(TestProvider.getValue(TokenType.INVALID))
        }

        @Test
        fun `type returns OPERATOR for arithmetic operators`() {
            assertEquals(TokenType.OPERATOR, TestProvider.type("+"))
            assertEquals(TokenType.OPERATOR, TestProvider.type("-"))
            assertEquals(TokenType.OPERATOR, TestProvider.type("*"))
            assertEquals(TokenType.OPERATOR, TestProvider.type("/"))
            assertEquals(TokenType.OPERATOR, TestProvider.type("=="))
        }

        @Test
        fun `type returns SYMBOL for punctuation`() {
            assertEquals(TokenType.SYMBOL, TestProvider.type(";"))
            assertEquals(TokenType.SYMBOL, TestProvider.type(":"))
            assertEquals(TokenType.SYMBOL, TestProvider.type("="))
        }

        @Test
        fun `type returns KEYWORD for let`() {
            assertEquals(TokenType.KEYWORD, TestProvider.type("let"))
        }

        @Test
        fun `type returns VARIABLE_TYPE for built-in types`() {
            assertEquals(TokenType.VARIABLE_TYPE, TestProvider.type("string"))
            assertEquals(TokenType.VARIABLE_TYPE, TestProvider.type("number"))
        }

        @Test
        fun `type returns IDENTIFIER for valid identifiers`() {
            assertEquals(TokenType.IDENTIFIER, TestProvider.type("myVar"))
            assertEquals(TokenType.IDENTIFIER, TestProvider.type("_x"))
            assertEquals(TokenType.IDENTIFIER, TestProvider.type("CamelCase"))
        }

        @Test
        fun `type returns NUMBER for integer literals`() {
            assertEquals(TokenType.NUMBER, TestProvider.type("0"))
            assertEquals(TokenType.NUMBER, TestProvider.type("123"))
        }

        @Test
        fun `type returns STRING for quoted literals`() {
            assertEquals(TokenType.STRING, TestProvider.type("\"hello\""))
            assertEquals(TokenType.STRING, TestProvider.type("\"\""))
        }

        @Test
        fun `type returns INVALID for unknown lexemes`() {
            assertEquals(TokenType.INVALID, TestProvider.type("@@@"))
            assertEquals(TokenType.INVALID, TestProvider.type(""))
        }
    }

    // -----------------------------------------------------------------------
    // TokenType enum completeness
    // -----------------------------------------------------------------------
    @Nested
    inner class TokenTypeEnum {

        @Test
        fun `all expected token types exist`() {
            val expected = setOf(
                "OPERATOR", "SYMBOL", "IDENTIFIER",
                "STRING", "NUMBER", "KEYWORD",
                "VARIABLE_TYPE", "INVALID"
            )
            val actual = TokenType.entries.map { it.name }.toSet()

            assertEquals(expected, actual)
        }
    }
}
