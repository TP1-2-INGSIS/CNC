package cnc.parser.flow

import cnc.common.Failure
import cnc.common.Position
import cnc.common.Success
import cnc.common.asCursor
import cnc.parser.cursor.flow
import cnc.parser.cursor.matches
import cnc.token.SymbolTokenDef
import cnc.token.Token
import cnc.token.TokenType
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class ParseFlowTest {

    private val kwLet = SymbolTokenDef("let", "let")
    private val colon = SymbolTokenDef("colon", ":")
    private val assign = SymbolTokenDef("assign", "=")
    private val semicolon = SymbolTokenDef("semicolon", ";")

    private fun tok(type: TokenType, text: String, row: Int = 1, col: Int = 0) =
        Token(type, Position(row, col), text)

    @Test
    fun `happy path completes all steps and executes then block`() {
        val tokens = listOf(
            tok(TokenType.KEYWORD, "let", row = 1, col = 0),
            tok(TokenType.IDENTIFIER, "x", row = 1, col = 4),
            tok(TokenType.SYMBOL, ":", row = 1, col = 5),
            tok(TokenType.VARIABLE_TYPE, "number", row = 1, col = 7),
            tok(TokenType.SYMBOL, ";", row = 1, col = 13)
        ).asCursor()

        var capturedName = ""
        var capturedType = ""

        val result = tokens.flow()
            .expect(kwLet)
            .capture(TokenType.IDENTIFIER) { capturedName = it.text }
            .expect(colon)
            .capture(TokenType.VARIABLE_TYPE) { capturedType = it.text }
            .expect(semicolon)
            .then("declaration") {
                Pair(capturedName, capturedType)
            }

        assertTrue(result is Success)
        assertEquals("declaration", result.msg)
        assertEquals(Pair("x", "number"), (result as Success).data)
        assertFalse(tokens.hasMore())
    }

    @Test
    fun `mismatch in expect transitions to failed state and skips subsequent operations`() {
        val tokens = listOf(
            tok(TokenType.KEYWORD, "let", row = 1, col = 0),
            tok(TokenType.IDENTIFIER, "x", row = 1, col = 4),
            tok(TokenType.SYMBOL, "=", row = 1, col = 6) // Esperaba ':'
        ).asCursor()

        var executedThen = false
        var capturedType = ""

        val result = tokens.flow()
            .expect(kwLet)
            .expect(TokenType.IDENTIFIER)
            .expect(colon)
            .capture(TokenType.VARIABLE_TYPE) { capturedType = it.text }
            .then {
                executedThen = true
                "should not happen"
            }

        assertTrue(result is Failure)
        val failure = result as Failure
        assertTrue(failure.msg.contains("row 1, col 6"))
        assertTrue(failure.msg.contains("expected ':' but found '='"))
        assertFalse(executedThen)
        assertEquals("", capturedType)
    }

    @Test
    fun `unexpected end of file produces clear failure message`() {
        val tokens = listOf(
            tok(TokenType.KEYWORD, "let", row = 1, col = 0)
        ).asCursor()

        val result = tokens.flow()
            .expect(kwLet)
            .expect(colon)
            .then { "ok" }

        assertTrue(result is Failure)
        val failure = result as Failure
        assertTrue(failure.msg.contains("unexpected end of file"))
        assertTrue(failure.msg.contains("expected ':'"))
    }

    @Test
    fun `optional consumes when present and does nothing when absent`() {
        val withAssign = listOf(
            tok(TokenType.SYMBOL, "=", row = 1, col = 0),
            tok(TokenType.SYMBOL, ";", row = 1, col = 2)
        ).asCursor()

        var hadAssign = false
        val res1 = withAssign.flow()
            .optional(assign) { hadAssign = true }
            .expect(semicolon)
            .then { "ok" }

        assertTrue(res1 is Success)
        assertTrue(hadAssign)

        val withoutAssign = listOf(
            tok(TokenType.SYMBOL, ";", row = 1, col = 0)
        ).asCursor()

        var hadAssign2 = false
        val res2 = withoutAssign.flow()
            .optional(assign) { hadAssign2 = true }
            .expect(semicolon)
            .then { "ok" }

        assertTrue(res2 is Success)
        assertFalse(hadAssign2)
    }

    @Test
    fun `cursor matches extensions test LL1 and LL2 lookahead`() {
        val tokens = listOf(
            tok(TokenType.IDENTIFIER, "myVar", row = 1, col = 0),
            tok(TokenType.SYMBOL, "=", row = 1, col = 6)
        ).asCursor()

        assertTrue(tokens.matches(TokenType.IDENTIFIER))
        assertTrue(tokens.matches(TokenType.IDENTIFIER, assign))
        assertFalse(tokens.matches(TokenType.IDENTIFIER, colon))
        assertFalse(tokens.matches(kwLet))
    }

    @Test
    fun `catch handler allows intercepting and customizing failure`() {
        val tokens = listOf(
            tok(TokenType.IDENTIFIER, "bad", row = 1, col = 0)
        ).asCursor()

        val result = tokens.flow()
            .expect(kwLet)
            .catch { Failure("Custom error: ${it.msg}", it.type) }
            .then { "ok" }

        assertTrue(result is Failure)
        assertTrue((result as Failure).msg.startsWith("Custom error:"))
    }

    @Test
    fun `parseBlock parses statements inside braces and captures BlockStatement`() {
        val openBrace = SymbolTokenDef("open_brace", "{")
        val closeBrace = SymbolTokenDef("close_brace", "}")

        val dummyStmt = cnc.ast.Call("test", emptyList())
        val tokens = listOf(
            tok(TokenType.SYMBOL, "{", row = 1, col = 0),
            tok(TokenType.IDENTIFIER, "test", row = 1, col = 2),
            tok(TokenType.SYMBOL, "}", row = 1, col = 7)
        ).asCursor()

        var capturedBlock: cnc.ast.BlockStatement? = null
        val result = tokens.flow()
            .parseBlock(
                openBrace = openBrace,
                closeBrace = closeBrace,
                statementParser = { cur ->
                    cur.advance() // consume "test"
                    Success("ok", dummyStmt)
                }
            ) { block ->
                capturedBlock = block
            }
            .then { "blockOk" }

        assertTrue(result is Success)
        assertEquals(1, capturedBlock?.statements?.size)
        assertEquals(dummyStmt, capturedBlock?.statements?.first())
    }
}

