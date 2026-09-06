package cnc.common

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class LookaheadStreamTest {

    @Test
    fun `empty stream returns hasMore false and null on peek and advance`() {
        val stream = emptySequence<Char>().asLookaheadStream()

        assertFalse(stream.hasMore())
        assertNull(stream.peek())
        assertNull(stream.peek(1))
        assertNull(stream.advance())
    }

    @Test
    fun `peek does not consume elements`() {
        val stream = listOf("a", "b", "c").asLookaheadStream()

        assertTrue(stream.hasMore())
        assertEquals("a", stream.peek(0))
        assertEquals("b", stream.peek(1))
        assertEquals("c", stream.peek(2))
        assertNull(stream.peek(3))

        // Cursor did not advance
        assertEquals("a", stream.peek(0))
        assertEquals("a", stream.advance())
        assertEquals("b", stream.peek(0))
    }

    @Test
    fun `advance consumes sequentially`() {
        val stream = listOf(1, 2, 3).asLookaheadStream()

        assertEquals(1, stream.advance())
        assertEquals(2, stream.advance())
        assertEquals(3, stream.advance())
        assertNull(stream.advance())
        assertFalse(stream.hasMore())
    }

    @Test
    fun `interleaving peek and advance`() {
        val stream = "hello".asSequence().asLookaheadStream()

        assertEquals('h', stream.peek(0))
        assertEquals('e', stream.peek(1))
        assertEquals('h', stream.advance())

        assertEquals('e', stream.peek(0))
        assertEquals('l', stream.peek(1))
        assertEquals('e', stream.advance())
        assertEquals('l', stream.advance())
        assertEquals('l', stream.advance())
        assertEquals('o', stream.advance())

        assertNull(stream.advance())
        assertFalse(stream.hasMore())
    }

    @Test
    fun `negative offset safely returns null`() {
        val stream = listOf(1).asLookaheadStream()
        assertNull(stream.peek(-1))
        assertNull(stream.peek(-5))
    }
}
