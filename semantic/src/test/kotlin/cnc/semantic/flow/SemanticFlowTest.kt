package cnc.semantic.flow

import cnc.common.ErrorType
import cnc.common.Failure
import cnc.common.Success
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class SemanticFlowTest {
    @Test
    fun `successful flow completes with thenOk`() {
        var sideEffect = false
        val result =
            SemanticFlow
                .ok()
                .expect(true) { "should not fail" }
                .step({ Success("ok", 42) }) { sideEffect = (it == 42) }
                .thenOk()

        assertTrue(result is Success)
        assertTrue(sideEffect)
    }

    @Test
    fun `expect transition to failure short-circuits subsequent operations`() {
        var called = false
        val result =
            SemanticFlow
                .ok()
                .expect(false) { "Expected condition failed" }
                .step({
                    called = true
                    Success("ok", "hello")
                })
                .thenOk()

        assertTrue(result is Failure)
        assertEquals("Expected condition failed", (result as Failure).msg)
        assertEquals(ErrorType.SEMANTIC, result.type)
        assertFalse(called)
    }

    @Test
    fun `step transition to failure propagates original failure details`() {
        val failureResult: Failure<Int> = Failure("Original error", ErrorType.SEMANTIC)
        var nextStepCalled = false

        val result =
            SemanticFlow
                .ok()
                .step({ failureResult })
                .step({
                    nextStepCalled = true
                    Success("ok", "value")
                })
                .thenOk()

        assertTrue(result is Failure)
        assertEquals("Original error", (result as Failure).msg)
        assertFalse(nextStepCalled)
    }

    @Test
    fun `stepAll succeeds when all items produce success`() {
        val captured = mutableListOf<String>()
        val result =
            SemanticFlow
                .ok()
                .stepAll(listOf(1, 2, 3), { item -> Success("ok", "item-$item") }) { captured.addAll(it) }
                .then { captured.size }

        assertTrue(result is Success)
        assertEquals(3, (result as Success).data)
        assertEquals(listOf("item-1", "item-2", "item-3"), captured)
    }

    @Test
    fun `stepAll stops and propagates failure on first failed item`() {
        var thirdEvaluated = false
        val result =
            SemanticFlow
                .ok()
                .stepAll(listOf(1, 2, 3), { item ->
                    when (item) {
                        2 -> Failure("Item 2 failed", ErrorType.SEMANTIC)
                        3 -> {
                            thirdEvaluated = true
                            Success("ok", "item-3")
                        }
                        else -> Success("ok", "item-$item")
                    }
                })
                .thenOk()

        assertTrue(result is Failure)
        assertEquals("Item 2 failed", (result as Failure).msg)
        assertFalse(thirdEvaluated)
    }

    @Test
    fun `optionalStep skips when target is null`() {
        var called = false
        val nullItem: String? = null
        val result =
            SemanticFlow
                .ok()
                .optionalStep(nullItem, { Success("ok", "should not run") }) { called = true }
                .thenOk()

        assertTrue(result is Success)
        assertFalse(called)
    }

    @Test
    fun `optionalStep executes when target is non-null`() {
        var captured = ""
        val item = "present"
        val result =
            SemanticFlow
                .ok()
                .optionalStep(item, { Success("ok", it.uppercase()) }) { captured = it }
                .thenOk()

        assertTrue(result is Success)
        assertEquals("PRESENT", captured)
    }

    @Test
    fun `thenResult returns operation result on success and failure on failed flow`() {
        val okRes =
            SemanticFlow
                .ok()
                .thenResult { Success("ok", "done") }
        assertTrue(okRes is Success)
        assertEquals("done", (okRes as Success).data)

        val failRes =
            SemanticFlow
                .failed(Failure("Pre-failed", ErrorType.SEMANTIC))
                .thenResult { Success("ok", "unreachable") }
        assertTrue(failRes is Failure)
        assertEquals("Pre-failed", (failRes as Failure).msg)
    }
}
