package cnc.token

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

class CncTokensTest {

    @Nested
    inner class CncKeywordsTests {

        @Test
        fun `verify individual keyword token definitions`() {
            assertTrue(CncKeywords.LET.match("let"))
            assertEquals("let", CncKeywords.LET.alias)

            assertTrue(CncKeywords.CONST.match("const"))
            assertEquals("const", CncKeywords.CONST.alias)

            assertTrue(CncKeywords.IF.match("if"))
            assertEquals("if", CncKeywords.IF.alias)

            assertTrue(CncKeywords.ELSE.match("else"))
            assertEquals("else", CncKeywords.ELSE.alias)

            assertTrue(CncKeywords.TRUE.match("true"))
            assertEquals("true", CncKeywords.TRUE.alias)

            assertTrue(CncKeywords.FALSE.match("false"))
            assertEquals("false", CncKeywords.FALSE.alias)

            assertTrue(CncKeywords.TYPE_STRING.match("string"))
            assertEquals("string_type", CncKeywords.TYPE_STRING.alias)

            assertTrue(CncKeywords.TYPE_NUMBER.match("number"))
            assertEquals("number_type", CncKeywords.TYPE_NUMBER.alias)

            assertTrue(CncKeywords.TYPE_BOOLEAN.match("boolean"))
            assertEquals("boolean_type", CncKeywords.TYPE_BOOLEAN.alias)

            assertFalse(CncKeywords.LET.match("var"))
        }

        @Test
        fun `v1_0 keyword map contains v1_0 syntax only`() {
            val map = CncKeywords.v1_0
            assertEquals(TokenType.KEYWORD, map["let"])
            assertEquals(TokenType.VARIABLE_TYPE, map["string"])
            assertEquals(TokenType.VARIABLE_TYPE, map["number"])

            assertFalse(map.containsKey("const"))
            assertFalse(map.containsKey("if"))
            assertFalse(map.containsKey("else"))
            assertFalse(map.containsKey("boolean"))
        }

        @Test
        fun `v1_1 keyword map contains v1_1 additions`() {
            val map = CncKeywords.v1_1
            assertEquals(TokenType.KEYWORD, map["let"])
            assertEquals(TokenType.KEYWORD, map["const"])
            assertEquals(TokenType.KEYWORD, map["if"])
            assertEquals(TokenType.KEYWORD, map["else"])
            assertEquals(TokenType.KEYWORD, map["true"])
            assertEquals(TokenType.KEYWORD, map["false"])
            assertEquals(TokenType.VARIABLE_TYPE, map["string"])
            assertEquals(TokenType.VARIABLE_TYPE, map["number"])
            assertEquals(TokenType.VARIABLE_TYPE, map["boolean"])
        }
    }

    @Nested
    inner class CncSymbolsTests {

        @Test
        fun `verify individual symbol token definitions`() {
            assertTrue(CncSymbols.PLUS.match("+"))
            assertTrue(CncSymbols.MINUS.match("-"))
            assertTrue(CncSymbols.DIVISION.match("/"))
            assertTrue(CncSymbols.MULTIPLICATION.match("*"))
            assertTrue(CncSymbols.EQUALS.match("=="))
            assertTrue(CncSymbols.EXPONENT.match("**"))
            assertTrue(CncSymbols.SEMICOLON.match(";"))
            assertTrue(CncSymbols.COLON.match(":"))
            assertTrue(CncSymbols.ASSIGN.match("="))
            assertTrue(CncSymbols.OPEN_PAREN.match("("))
            assertTrue(CncSymbols.CLOSE_PAREN.match(")"))
            assertTrue(CncSymbols.OPEN_BRACE.match("{"))
            assertTrue(CncSymbols.CLOSE_BRACE.match("}"))
            assertTrue(CncSymbols.COMMA.match(","))

            assertFalse(CncSymbols.PLUS.match("-"))
        }

        @Test
        fun `all map contains expected operators and symbols`() {
            val all = CncSymbols.all

            assertEquals(TokenType.OPERATOR, all["+"])
            assertEquals(TokenType.OPERATOR, all["-"])
            assertEquals(TokenType.OPERATOR, all["/"])
            assertEquals(TokenType.OPERATOR, all["*"])
            assertEquals(TokenType.OPERATOR, all["=="])
            assertEquals(TokenType.OPERATOR, all["**"])

            assertEquals(TokenType.SYMBOL, all[";"])
            assertEquals(TokenType.SYMBOL, all[":"])
            assertEquals(TokenType.SYMBOL, all["="])
            assertEquals(TokenType.SYMBOL, all["("])
            assertEquals(TokenType.SYMBOL, all[")"])
            assertEquals(TokenType.SYMBOL, all["{"])
            assertEquals(TokenType.SYMBOL, all["}"])
            assertEquals(TokenType.SYMBOL, all[","])
        }
    }

    @Nested
    inner class CncPatternsTests {

        @Test
        fun `identifier pattern matches valid identifiers`() {
            val pattern = CncPatterns.IDENTIFIER
            assertEquals("identifier", pattern.alias)

            assertTrue(pattern.match("myVar"))
            assertTrue(pattern.match("_privateVar"))
            assertTrue(pattern.match("var123"))
            assertTrue(pattern.match("x"))

            assertFalse(pattern.match("123var"))
            assertFalse(pattern.match(""))
            assertFalse(pattern.match("my-var"))
        }

        @Test
        fun `number pattern matches integers and decimals`() {
            val pattern = CncPatterns.NUMBER
            assertEquals("number_exp", pattern.alias)

            assertTrue(pattern.match("42"))
            assertTrue(pattern.match("0"))
            assertTrue(pattern.match("3.14159"))
            assertTrue(pattern.match("100.0"))

            assertFalse(pattern.match("abc"))
            assertFalse(pattern.match(""))
        }

        @Test
        fun `string pattern matches double-quoted strings`() {
            val pattern = CncPatterns.STRING
            assertEquals("string_exp", pattern.alias)

            assertTrue(pattern.match("\"hello world\""))
            assertTrue(pattern.match("\"\""))
            assertTrue(pattern.match("\"123\""))

            assertFalse(pattern.match("hello"))
            assertFalse(pattern.match("\"unclosed"))
        }
    }
}
