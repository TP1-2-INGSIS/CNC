package cnc.lexer

import cnc.common.Position
import cnc.common.StrContent
import cnc.lexer.rules.StandardRules
import cnc.lexer.rules.TrieRule
import cnc.token.TokenType
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.io.StringReader

class LexerTest {

    private val testKeywords = mapOf(
        "let" to TokenType.KEYWORD,
        "string" to TokenType.VARIABLE_TYPE,
        "number" to TokenType.VARIABLE_TYPE
    )

    private val testSymbols = mapOf(
        "==" to TokenType.OPERATOR,
        "=" to TokenType.SYMBOL,
        "+" to TokenType.OPERATOR,
        "-" to TokenType.OPERATOR,
        "*" to TokenType.OPERATOR,
        "/" to TokenType.OPERATOR,
        "**" to TokenType.OPERATOR,
        ";" to TokenType.SYMBOL,
        ":" to TokenType.SYMBOL
    )

    private val testRules = listOf(
        StandardRules.whitespace(),
        StandardRules.doubleQuotedString(TokenType.STRING),
        StandardRules.integerNumber(TokenType.NUMBER),
        StandardRules.standardIdentifier(keywords = testKeywords),
        TrieRule(testSymbols)
    )

    private val lexer = Lexer(testRules)

    private fun lex(input: String) = lexer.tokenize(StrContent(input)).toList()

    // -------------------------------------------------------------------------
    // Trie Unit Tests
    // -------------------------------------------------------------------------

    @Nested
    inner class TrieTests {

        @Test
        fun `matchLongest selects longest prefix`() {
            val trie = buildTrie(
                mapOf(
                    "=" to TokenType.SYMBOL,
                    "==" to TokenType.OPERATOR,
                    "*" to TokenType.OPERATOR,
                    "**" to TokenType.OPERATOR
                )
            )

            val streamEquals = CharStream(StringReader("=="))
            val matchEquals = trie.matchLongest(streamEquals)
            assertEquals(TokenType.OPERATOR to 2, matchEquals)

            val streamSingle = CharStream(StringReader("= "))
            val matchSingle = trie.matchLongest(streamSingle)
            assertEquals(TokenType.SYMBOL to 1, matchSingle)

            val streamExponent = CharStream(StringReader("**="))
            val matchExponent = trie.matchLongest(streamExponent)
            assertEquals(TokenType.OPERATOR to 2, matchExponent)
        }

        @Test
        fun `matchExact returns value only for exact match`() {
            val trie = buildTrie(testKeywords)
            assertEquals(TokenType.KEYWORD, trie.matchExact("let"))
            assertEquals(TokenType.VARIABLE_TYPE, trie.matchExact("string"))
            assertNull(trie.matchExact("letter"))
            assertNull(trie.matchExact("le"))
        }
    }

    // -------------------------------------------------------------------------
    // CharStream Unit Tests
    // -------------------------------------------------------------------------

    @Nested
    inner class CharStreamTests {

        @Test
        fun `peek and advance track position correctly across newlines`() {
            val stream = CharStream(StringReader("a\nbc"))
            assertEquals(Position(0, 0), stream.position)
            assertEquals('a', stream.peek())
            assertEquals('a', stream.advance())

            assertEquals(Position(0, 1), stream.position)
            assertEquals('\n', stream.advance())

            assertEquals(Position(1, 0), stream.position)
            assertEquals('b', stream.advance())

            assertEquals(Position(1, 1), stream.position)
            assertEquals('c', stream.advance())

            assertEquals(Position(1, 2), stream.position)
            assertNull(stream.advance())
            assertEquals(false, stream.hasMore())
        }

        @Test
        fun `consume extracts exact number of characters`() {
            val stream = CharStream(StringReader("hello world"))
            assertEquals("hello", stream.consume(5))
            assertEquals(Position(0, 5), stream.position)
            assertEquals(' ', stream.peek())
        }
    }

    // -------------------------------------------------------------------------
    // TokenType Tests
    // -------------------------------------------------------------------------

    @Nested
    inner class TokenTypes {

        @Test fun `let es KEYWORD`() {
            val tokens = lex("let")
            assertEquals(1, tokens.size)
            assertEquals(TokenType.KEYWORD, tokens[0].type)
        }

        @Test fun `identificador simple es IDENTIFIER`() {
            val tokens = lex("myVar")
            assertEquals(1, tokens.size)
            assertEquals(TokenType.IDENTIFIER, tokens[0].type)
        }

        @Test fun `identificador con guion bajo es IDENTIFIER`() {
            val tokens = lex("_my_var_2")
            assertEquals(1, tokens.size)
            assertEquals(TokenType.IDENTIFIER, tokens[0].type)
        }

        @Test fun `string (tipo) es VARIABLE_TYPE`() {
            assertEquals(TokenType.VARIABLE_TYPE, lex("string")[0].type)
        }

        @Test fun `number (tipo) es VARIABLE_TYPE`() {
            assertEquals(TokenType.VARIABLE_TYPE, lex("number")[0].type)
        }

        @Test fun `literal entero es NUMBER`() {
            val tokens = lex("42")
            assertEquals(1, tokens.size)
            assertEquals(TokenType.NUMBER, tokens[0].type)
        }

        @Test fun `literal de string es STRING`() {
            val tokens = lex("\"hello world\"")
            assertEquals(1, tokens.size)
            assertEquals(TokenType.STRING, tokens[0].type)
        }

        @Test fun `operador suma es OPERATOR`() {
            assertEquals(TokenType.OPERATOR, lex("+")[0].type)
        }

        @Test fun `operador resta es OPERATOR`() {
            assertEquals(TokenType.OPERATOR, lex("-")[0].type)
        }

        @Test fun `operador multiplicacion es OPERATOR`() {
            assertEquals(TokenType.OPERATOR, lex("*")[0].type)
        }

        @Test fun `operador division es OPERATOR`() {
            assertEquals(TokenType.OPERATOR, lex("/")[0].type)
        }

        @Test fun `operador igualdad es OPERATOR`() {
            assertEquals(TokenType.OPERATOR, lex("==")[0].type)
        }

        @Test fun `asignacion es SYMBOL`() {
            assertEquals(TokenType.SYMBOL, lex("=")[0].type)
        }

        @Test fun `dos puntos es SYMBOL`() {
            assertEquals(TokenType.SYMBOL, lex(":")[0].type)
        }

        @Test fun `punto y coma es SYMBOL`() {
            assertEquals(TokenType.SYMBOL, lex(";")[0].type)
        }
    }

    // -------------------------------------------------------------------------
    // Token Text Tests
    // -------------------------------------------------------------------------

    @Nested
    inner class TokenText {

        @Test fun `texto de keyword preservado`() {
            assertEquals("let", lex("let")[0].text)
        }

        @Test fun `texto de identificador preservado`() {
            assertEquals("myVar", lex("myVar")[0].text)
        }

        @Test fun `texto de numero preservado`() {
            assertEquals("123", lex("123")[0].text)
        }

        @Test fun `texto de string incluye comillas`() {
            assertEquals("\"hello\"", lex("\"hello\"")[0].text)
        }

        @Test fun `texto de operador preservado`() {
            assertEquals("+", lex("+")[0].text)
        }
    }

    // -------------------------------------------------------------------------
    // Positions Tests
    // -------------------------------------------------------------------------

    @Nested
    inner class Positions {

        @Test fun `primer token empieza en columna 0`() {
            val tokens = lex("let")
            assertEquals(Position(0, 0), tokens[0].pos)
        }

        @Test fun `fila es 0 para la primera linea`() {
            assertEquals(0, lex("let")[0].pos.row)
        }

        @Test fun `columna refleja el offset en la linea`() {
            val tokens = lex("let x")
            assertEquals(4, tokens[1].pos.col)
        }

        @Test fun `columnas correctas en declaracion completa`() {
            val tokens = lex("let x : number =")
            assertEquals(0,  tokens[0].pos.col) // let
            assertEquals(4,  tokens[1].pos.col) // x
            assertEquals(6,  tokens[2].pos.col) // :
            assertEquals(8,  tokens[3].pos.col) // number
            assertEquals(15, tokens[4].pos.col) // =
        }

        @Test fun `todos los tokens de la primera linea tienen row 0`() {
            val tokens = lex("let x : number = 42;")
            assertTrue(tokens.all { it.pos.row == 0 })
        }

        @Test fun `tokens en multiples lineas actualizan row y col`() {
            val tokens = lex("let x = 1;\nlet y = 2;")
            assertEquals(Position(0, 0), tokens[0].pos) // let (line 0)
            assertEquals(Position(0, 4), tokens[1].pos) // x
            assertEquals(Position(1, 0), tokens[5].pos) // let (line 1)
            assertEquals(Position(1, 4), tokens[6].pos) // y
        }
    }

    // -------------------------------------------------------------------------
    // Full Statements Tests
    // -------------------------------------------------------------------------

    @Nested
    inner class FullStatements {

        @Test fun `declaracion de numero produce 7 tokens`() {
            assertEquals(7, lex("let x: number = 42;").size)
        }

        @Test fun `declaracion de string produce 7 tokens`() {
            assertEquals(7, lex("let name: string = \"John\";").size)
        }

        @Test fun `orden de tipos en declaracion de numero`() {
            val types = lex("let x: number = 42;").map { it.type }
            assertEquals(
                listOf(
                    TokenType.KEYWORD,
                    TokenType.IDENTIFIER,
                    TokenType.SYMBOL,      // :
                    TokenType.VARIABLE_TYPE,
                    TokenType.SYMBOL,      // =
                    TokenType.NUMBER,
                    TokenType.SYMBOL       // ;
                ),
                types
            )
        }

        @Test fun `orden de tipos en declaracion de string`() {
            val types = lex("let name: string = \"John\";").map { it.type }
            assertEquals(
                listOf(
                    TokenType.KEYWORD,
                    TokenType.IDENTIFIER,
                    TokenType.SYMBOL,      // :
                    TokenType.VARIABLE_TYPE,
                    TokenType.SYMBOL,      // =
                    TokenType.STRING,
                    TokenType.SYMBOL       // ;
                ),
                types
            )
        }

        @Test fun `expresion aritmetica simple`() {
            val types = lex("a + b").map { it.type }
            assertEquals(
                listOf(TokenType.IDENTIFIER, TokenType.OPERATOR, TokenType.IDENTIFIER),
                types
            )
        }

        @Test fun `expresion aritmetica compleja`() {
            val types = lex("a + b * c - d / e").map { it.type }
            assertEquals(
                listOf(
                    TokenType.IDENTIFIER, TokenType.OPERATOR,   // a +
                    TokenType.IDENTIFIER, TokenType.OPERATOR,   // b *
                    TokenType.IDENTIFIER, TokenType.OPERATOR,   // c -
                    TokenType.IDENTIFIER, TokenType.OPERATOR,   // d /
                    TokenType.IDENTIFIER                        // e
                ),
                types
            )
        }
    }

    // -------------------------------------------------------------------------
    // Edge Cases
    // -------------------------------------------------------------------------

    @Nested
    inner class EdgeCases {

        @Test fun `entrada vacia produce cero tokens`() {
            assertEquals(0, lex("").size)
        }

        @Test fun `espacios extra son ignorados`() {
            val tokens = lex("let   x")
            assertEquals(2, tokens.size)
            assertEquals(TokenType.KEYWORD, tokens[0].type)
            assertEquals(TokenType.IDENTIFIER, tokens[1].type)
        }

        @Test fun `let no es IDENTIFIER`() {
            assertEquals(TokenType.KEYWORD, lex("let")[0].type)
        }

        @Test fun `letter es IDENTIFIER y no se parte con let`() {
            val tokens = lex("letter")
            assertEquals(1, tokens.size)
            assertEquals(TokenType.IDENTIFIER, tokens[0].type)
            assertEquals("letter", tokens[0].text)
        }

        @Test fun `string y number son VARIABLE_TYPE, no IDENTIFIER`() {
            assertEquals(TokenType.VARIABLE_TYPE, lex("string")[0].type)
            assertEquals(TokenType.VARIABLE_TYPE, lex("number")[0].type)
        }

        @Test fun `stringVar es IDENTIFIER y no se confunde con el tipo string`() {
            val tokens = lex("stringVar")
            assertEquals(1, tokens.size)
            assertEquals(TokenType.IDENTIFIER, tokens[0].type)
            assertEquals("stringVar", tokens[0].text)
        }

        @Test fun `string literal vacio`() {
            val tokens = lex("\"\"")
            assertEquals(1, tokens.size)
            assertEquals(TokenType.STRING, tokens[0].type)
            assertEquals("\"\"", tokens[0].text)
        }

        @Test fun `string literal con espacios`() {
            val tokens = lex("\"hello world\"")
            assertEquals(1, tokens.size)
            assertEquals(TokenType.STRING, tokens[0].type)
        }

        @Test fun `numero de multiples digitos`() {
            val tokens = lex("12345")
            assertEquals(1, tokens.size)
            assertEquals(TokenType.NUMBER, tokens[0].type)
            assertEquals("12345", tokens[0].text)
        }

        @Test fun `solo simbolos`() {
            val types = lex(": ; =").map { it.type }
            assertEquals(
                listOf(TokenType.SYMBOL, TokenType.SYMBOL, TokenType.SYMBOL),
                types
            )
        }

        @Test fun `operador == no se confunde con asignacion =`() {
            val tokens = lex("==")
            assertEquals(1, tokens.size)
            assertEquals(TokenType.OPERATOR, tokens[0].type)
            assertEquals("==", tokens[0].text)
        }

        @Test fun `operador ** no se confunde con *`() {
            val tokens = lex("**")
            assertEquals(1, tokens.size)
            assertEquals(TokenType.OPERATOR, tokens[0].type)
            assertEquals("**", tokens[0].text)
        }

        @Test fun `identificador con numeros al final`() {
            val tokens = lex("var123")
            assertEquals(1, tokens.size)
            assertEquals(TokenType.IDENTIFIER, tokens[0].type)
        }

        @Test fun `caracteres invalidos emiten token INVALID`() {
            val tokens = lex("@")
            assertEquals(1, tokens.size)
            assertEquals(TokenType.INVALID, tokens[0].type)
            assertEquals("@", tokens[0].text)
        }
    }
}
