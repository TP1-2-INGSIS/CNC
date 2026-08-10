package org.lexer

import org.config.Token
import org.config.TokenType
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.utils.Position

class LexerTest {
    val lexer = CharLexer()

    private fun lex(input: String): List<Token> =
        lexer.getTokens(StrContent(input)).toList()

    // -------------------------------------------------------------------------
    // TokenType
    // -------------------------------------------------------------------------

    @Nested
    inner class TokenTypes {

        @Test fun `let keyword`() {
            val tokens = lex("let")
            assertEquals(1, tokens.size)
            assertEquals(TokenType.LET, tokens[0].type)
        }

        @Test fun `identifier simple`() {
            val tokens = lex("myVar")
            assertEquals(1, tokens.size)
            assertEquals(TokenType.IDENTIFIER, tokens[0].type)
        }

        @Test fun `identifier with underscore`() {
            val tokens = lex("_my_var_2")
            assertEquals(1, tokens.size)
            assertEquals(TokenType.IDENTIFIER, tokens[0].type)
        }

        @Test fun string() {
            val tokens = lex("string")
            assertEquals(TokenType.TYPE, tokens[0].type)
        }

        @Test fun number() {
            val tokens = lex("number")
            assertEquals(TokenType.TYPE, tokens[0].type)
        }

        @Test fun intLiteral() {
            val tokens = lex("42")
            assertEquals(1, tokens.size)
            assertEquals(TokenType.LITERAL, tokens[0].type)
        }

        @Test fun stringLiteral() {
            val tokens = lex("\"hello world\"")
            assertEquals(1, tokens.size)
            assertEquals(TokenType.STRING, tokens[0].type)
        }

        @Test fun `assign operator`() {
            val tokens = lex("=")
            assertEquals(TokenType.ASSIGN, tokens[0].type)
        }

        @Test fun `colon symbol`() {
            val tokens = lex(":")
            assertEquals(TokenType.COLON, tokens[0].type)
        }

        @Test fun `semicolon symbol`() {
            val tokens = lex(";")
            assertEquals(TokenType.SEMICOLON, tokens[0].type)
        }

        @Test fun `binary operator plus`() {
            val tokens = lex("+")
            assertEquals(TokenType.OPERATOR, tokens[0].type)
        }

        @Test fun `binary operator minus`() {
            val tokens = lex("-")
            assertEquals(TokenType.OPERATOR, tokens[0].type)
        }

        @Test fun `binary operator multiply`() {
            val tokens = lex("*")
            assertEquals(TokenType.OPERATOR, tokens[0].type)
        }

        @Test fun `binary operator divide`() {
            val tokens = lex("/")
            assertEquals(TokenType.OPERATOR, tokens[0].type)
        }
    }

    // -------------------------------------------------------------------------
    // Token text
    // -------------------------------------------------------------------------

    @Nested
    inner class TokenText {

        @Test fun `token text is preserved`() {
            val tokens = lex("let")
            assertEquals("let", tokens[0].text)
        }

        @Test fun `identifier text is preserved`() {
            val tokens = lex("myVar")
            assertEquals("myVar", tokens[0].text)
        }

        @Test fun `literal text is preserved`() {
            val tokens = lex("123")
            assertEquals("123", tokens[0].text)
        }

        @Test fun `string text includes quotes`() {
            val tokens = lex("\"hello\"")
            assertEquals("\"hello\"", tokens[0].text)
        }
    }

    // -------------------------------------------------------------------------
    // Positions
    // -------------------------------------------------------------------------

    @Nested
    inner class Positions {

        @Test fun `first token starts at column 1`() {
            val tokens = lex("let")
            assertEquals(Position(1, 1), tokens[0].pos)
        }

        @Test fun `row is 1-indexed`() {
            val tokens = lex("let x")
            assertEquals(1, tokens[0].pos.row)
        }

        @Test fun `column reflects offset in line`() {
            // "let x" -> 'x' empieza en col 5
            val tokens = lex("let x")
            assertEquals(5, tokens[1].pos.col)
        }

        @Test fun `multiple tokens have correct columns`() {
            // "let x : number ="
            //  0   4   6  8
            val tokens = lex("let x : number =")
            assertEquals(1,  tokens[0].pos.col) // let
            assertEquals(5,  tokens[1].pos.col) // x
            assertEquals(7,  tokens[2].pos.col) // :
            assertEquals(9,  tokens[3].pos.col) // number
            assertEquals(16, tokens[4].pos.col) // =
        }
    }

    // -------------------------------------------------------------------------
    // Full declarations
    // -------------------------------------------------------------------------

    @Nested
    inner class FullStatements {

        @Test fun `number declaration`() {
            val tokens = lex("let x: number = 42;")
            val types = tokens.map { it.type }
            assertEquals(
                listOf(
                    TokenType.LET,
                    TokenType.IDENTIFIER,
                    TokenType.COLON,
                    TokenType.TYPE,
                    TokenType.ASSIGN,
                    TokenType.LITERAL,
                    TokenType.SEMICOLON
                ),
                types
            )
        }

        @Test fun `string declaration`() {
            val tokens = lex("let name: string = \"John\";")
            val types = tokens.map { it.type }
            assertEquals(
                listOf(
                    TokenType.LET,
                    TokenType.IDENTIFIER,
                    TokenType.COLON,
                    TokenType.TYPE,
                    TokenType.ASSIGN,
                    TokenType.STRING,
                    TokenType.SEMICOLON
                ),
                types
            )
        }

        @Test fun `token count for number declaration`() {
            val tokens = lex("let x: number = 42;")
            assertEquals(7, tokens.size)
        }

        @Test fun `token count for string declaration`() {
            val tokens = lex("let name: string = \"John\";")
            assertEquals(7, tokens.size)
        }
    }

    // -------------------------------------------------------------------------
    // Edge cases
    // -------------------------------------------------------------------------

    @Nested
    inner class EdgeCases {

        @Test fun `empty input produces no tokens`() {
            val tokens = lex("")
            assertEquals(0, tokens.size)
        }

        @Test fun `extra spaces are ignored`() {
            val tokens = lex("let   x")
            assertEquals(2, tokens.size)
            assertEquals(TokenType.LET, tokens[0].type)
            assertEquals(TokenType.IDENTIFIER, tokens[1].type)
        }

        @Test fun `let is not an identifier`() {
            // "let" debe ser LET, no IDENTIFIER (el orden de las reglas importa)
            val tokens = lex("let")
            assertEquals(TokenType.LET, tokens[0].type)
        }

        @Test fun `string and number are not identifiers`() {
            assertEquals(TokenType.TYPE, lex("string")[0].type)
            assertEquals(TokenType.TYPE, lex("number")[0].type)
        }

        @Test fun `empty string literal`() {
            val tokens = lex("\"\"")
            assertEquals(1, tokens.size)
            assertEquals(TokenType.STRING, tokens[0].type)
        }
    }
}
