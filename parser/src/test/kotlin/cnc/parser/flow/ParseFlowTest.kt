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

    private fun tok(
        type: TokenType,
        text: String,
        row: Int = 1,
        col: Int = 0,
    ) = Token(type, Position(row, col), text)

    @Test
    fun `happy path completes all steps and executes then block`() {
        val tokens =
            listOf(
                tok(TokenType.KEYWORD, "let", row = 1, col = 0),
                tok(TokenType.IDENTIFIER, "x", row = 1, col = 4),
                tok(TokenType.SYMBOL, ":", row = 1, col = 5),
                tok(TokenType.VARIABLE_TYPE, "number", row = 1, col = 7),
                tok(TokenType.SYMBOL, ";", row = 1, col = 13),
            ).asCursor()

        var capturedName = ""
        var capturedType = ""

        val result =
            tokens
                .flow()
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
        val tokens =
            listOf(
                tok(TokenType.KEYWORD, "let", row = 1, col = 0),
                tok(TokenType.IDENTIFIER, "x", row = 1, col = 4),
                tok(TokenType.SYMBOL, "=", row = 1, col = 6), // Esperaba ':'
            ).asCursor()

        var executedThen = false
        var capturedType = ""

        val result =
            tokens
                .flow()
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
        val tokens =
            listOf(
                tok(TokenType.KEYWORD, "let", row = 1, col = 0),
            ).asCursor()

        val result =
            tokens
                .flow()
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
        val withAssign =
            listOf(
                tok(TokenType.SYMBOL, "=", row = 1, col = 0),
                tok(TokenType.SYMBOL, ";", row = 1, col = 2),
            ).asCursor()

        var hadAssign = false
        val res1 =
            withAssign
                .flow()
                .optional(assign) { hadAssign = true }
                .expect(semicolon)
                .then { "ok" }

        assertTrue(res1 is Success)
        assertTrue(hadAssign)

        val withoutAssign =
            listOf(
                tok(TokenType.SYMBOL, ";", row = 1, col = 0),
            ).asCursor()

        var hadAssign2 = false
        val res2 =
            withoutAssign
                .flow()
                .optional(assign) { hadAssign2 = true }
                .expect(semicolon)
                .then { "ok" }

        assertTrue(res2 is Success)
        assertFalse(hadAssign2)
    }

    @Test
    fun `cursor matches extensions test LL1 and LL2 lookahead`() {
        val tokens =
            listOf(
                tok(TokenType.IDENTIFIER, "myVar", row = 1, col = 0),
                tok(TokenType.SYMBOL, "=", row = 1, col = 6),
            ).asCursor()

        assertTrue(tokens.matches(TokenType.IDENTIFIER))
        assertTrue(tokens.matches(TokenType.IDENTIFIER, assign))
        assertFalse(tokens.matches(TokenType.IDENTIFIER, colon))
        assertFalse(tokens.matches(kwLet))
    }

    @Test
    fun `catch handler allows intercepting and customizing failure`() {
        val tokens =
            listOf(
                tok(TokenType.IDENTIFIER, "bad", row = 1, col = 0),
            ).asCursor()

        val result =
            tokens
                .flow()
                .expect(kwLet)
                .catch { Failure("Custom error: ${it.msg}", it.type) }
                .then { "ok" }

        assertTrue(result is Failure)
        assertTrue((result as Failure).msg.startsWith("Custom error:"))
    }

    @Test
    fun `step executes sub-parser and captures result on Success`() {
        val tokens =
            listOf(
                tok(TokenType.NUMBER, "42"),
                tok(TokenType.SYMBOL, ";"),
            ).asCursor()

        var capturedVal = 0
        val result =
            tokens
                .flow()
                .step({ cur ->
                    val tok = cur.advance()!!
                    Success("parsed number", tok.text.toInt())
                }) { capturedVal = it }
                .expect(semicolon)
                .then { capturedVal }

        assertTrue(result is Success)
        assertEquals(42, (result as Success).data)
    }

    @Test
    fun `step transitions to FailedFlow on sub-parser Failure`() {
        val tokens =
            listOf(
                tok(TokenType.KEYWORD, "invalid"),
                tok(TokenType.SYMBOL, ";"),
            ).asCursor()

        var capturedVal = 0
        val result =
            tokens
                .flow()
                .step({ _ ->
                    Failure<Int>("Sub-parser error", cnc.common.ErrorType.PARSER)
                }) { capturedVal = it }
                .expect(semicolon)
                .then { capturedVal }

        assertTrue(result is Failure)
        assertEquals(0, capturedVal)
        assertEquals("Sub-parser error", (result as Failure).msg)
    }

    @Test
    fun `optionalStep executes sub-parser when trigger matches`() {
        val tokens =
            listOf(
                tok(TokenType.SYMBOL, "="),
                tok(TokenType.NUMBER, "99"),
                tok(TokenType.SYMBOL, ";"),
            ).asCursor()

        val assignDef = SymbolTokenDef("assign", "=")
        var capturedVal: Int? = null

        val result =
            tokens
                .flow()
                .optionalStep(assignDef, { cur ->
                    val tok = cur.advance()!!
                    Success("parsed num", tok.text.toInt())
                }) { capturedVal = it }
                .expect(semicolon)
                .then { capturedVal }

        assertTrue(result is Success)
        assertEquals(99, (result as Success).data)
    }

    @Test
    fun `optionalStep skips execution without error when trigger does not match`() {
        val tokens =
            listOf(
                tok(TokenType.SYMBOL, ";"),
            ).asCursor()

        val assignDef = SymbolTokenDef("assign", "=")
        var capturedVal: Int? = null

        val result =
            tokens
                .flow()
                .optionalStep(assignDef, { cur ->
                    val tok = cur.advance()!!
                    Success("parsed num", tok.text.toInt())
                }) { capturedVal = it }
                .expect(semicolon)
                .then { capturedVal }

        assertTrue(result is Success)
        assertEquals(null, (result as Success).data)
    }

    @Test
    fun `captureAny captures matching token from list of definitions`() {
        val tokens =
            listOf(
                tok(TokenType.KEYWORD, "let"),
                tok(TokenType.SYMBOL, ";"),
            ).asCursor()

        val defLet = SymbolTokenDef("let", "let")
        val defConst = SymbolTokenDef("const", "const")

        var capturedKw = ""
        val result =
            tokens
                .flow()
                .captureAny(listOf(defLet, defConst)) { capturedKw = it.text }
                .expect(semicolon)
                .then { capturedKw }

        assertTrue(result is Success)
        assertEquals("let", (result as Success).data)
    }

    @Test
    fun `captureAny fails immediately when token does not match any definition`() {
        val tokens =
            listOf(
                tok(TokenType.KEYWORD, "while"),
                tok(TokenType.SYMBOL, ";"),
            ).asCursor()

        val defLet = SymbolTokenDef("let", "let")
        val defConst = SymbolTokenDef("const", "const")

        var capturedKw = ""
        val result =
            tokens
                .flow()
                .captureAny(listOf(defLet, defConst)) { capturedKw = it.text }
                .expect(semicolon)
                .then { capturedKw }

        assertTrue(result is Failure)
        assertEquals("", capturedKw)
        assertTrue((result as Failure).msg.contains("expected 'let or const' but found 'while'"))
    }

    @Test
    fun `expectAny matches and advances cursor`() {
        val tokens =
            listOf(
                tok(TokenType.KEYWORD, "const"),
                tok(TokenType.SYMBOL, ";"),
            ).asCursor()

        val defLet = SymbolTokenDef("let", "let")
        val defConst = SymbolTokenDef("const", "const")

        val result =
            tokens
                .flow()
                .expectAny(defLet, defConst)
                .expect(semicolon)
                .then { "ok" }

        assertTrue(result is Success)
        assertEquals("ok", (result as Success).data)
    }
}
