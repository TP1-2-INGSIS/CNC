package cnc.common

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class ResultTest {

    @Test
    fun `test success creation and properties`() {
        val success = Success("ok", 42)
        assertTrue(success.isOk())
        assertEquals("ok", success.msg)
        assertEquals(42, success.data)
    }

    @Test
    fun `test failure creation and properties`() {
        val failure = Failure<Int>("error occurred", ErrorType.INTERPRETER)
        assertFalse(failure.isOk())
        assertEquals("error occurred", failure.msg)
        assertEquals(ErrorType.INTERPRETER, failure.type)
    }

    @Test
    fun `test map on success and failure`() {
        val success: Result<Int> = Success("ok", 10)
        val mappedSuccess = success.map { it * 2 }
        assertTrue(mappedSuccess is Success)
        assertEquals(20, (mappedSuccess as Success).data)

        val failure: Result<Int> = Failure("bad", ErrorType.PARSER)
        val mappedFailure = failure.map { it * 2 }
        assertTrue(mappedFailure is Failure)
        assertEquals("bad", (mappedFailure as Failure).msg)
    }

    @Test
    fun `test flatMap on success and failure`() {
        val success: Result<Int> = Success("ok", 10)
        val flatMapped = success.flatMap { Success("ok", "$it items") }
        assertTrue(flatMapped is Success)
        assertEquals("10 items", (flatMapped as Success).data)

        val failure: Result<Int> = Failure("fail", ErrorType.SEMANTIC)
        val flatMappedFail = failure.flatMap { Success("ok", "$it items") }
        assertTrue(flatMappedFail is Failure)
        assertEquals("fail", (flatMappedFail as Failure).msg)
    }

    @Test
    fun `test onSuccess and onFailure callbacks`() {
        var successTriggered = false
        var failureTriggered = false

        val success: Result<String> = Success("ok", "hello")
        success.onSuccess { successTriggered = true }
        success.onFailure { failureTriggered = true }

        assertTrue(successTriggered)
        assertFalse(failureTriggered)

        successTriggered = false
        failureTriggered = false

        val failure: Result<String> = Failure("err", ErrorType.CLI)
        failure.onSuccess { successTriggered = true }
        failure.onFailure { failureTriggered = true }

        assertFalse(successTriggered)
        assertTrue(failureTriggered)
    }

    @Test
    fun `test error types enumeration`() {
        val types = ErrorType.entries
        assertTrue(types.contains(ErrorType.LEXER))
        assertTrue(types.contains(ErrorType.PARSER))
        assertTrue(types.contains(ErrorType.SEMANTIC))
        assertTrue(types.contains(ErrorType.CLI))
        assertTrue(types.contains(ErrorType.INTERPRETER))
    }

    @Test
    fun `test position advance`() {
        val pos = Position(1, 5)
        val advancedChar = pos.advance('a')
        assertEquals(1, advancedChar.row)
        assertEquals(6, advancedChar.col)

        val advancedNewline = pos.advance('\n')
        assertEquals(2, advancedNewline.row)
        assertEquals(0, advancedNewline.col)
    }

    @Test
    fun `test node structure`() {
        val node = Node<Int>(left = null, right = null)
        org.junit.jupiter.api.Assertions.assertNull(node.left)
        org.junit.jupiter.api.Assertions.assertNull(node.right)
    }
}
