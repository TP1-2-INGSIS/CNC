package cnc.common

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class CursorTest {

    @Test
    fun `empty stream returns null on peek and advance`() {
        val cursor = emptySequence<Char>().asCursor()

        assertFalse(cursor.hasMore())
        assertEquals(0, cursor.currentOffset)
        assertNull(cursor.peek())
        assertNull(cursor.advance())
        assertEquals(0, cursor.currentOffset)
    }

    @Test
    fun `peek inspects future elements without advancing`() {
        val cursor = sequenceOf('a', 'b', 'c').asCursor()

        assertEquals(0, cursor.currentOffset)
        assertEquals('a', cursor.peek(0))
        assertEquals('b', cursor.peek(1))
        assertEquals('c', cursor.peek(2))
        assertNull(cursor.peek(3))
        assertEquals(0, cursor.currentOffset)

        assertEquals('a', cursor.advance())
        assertEquals(1, cursor.currentOffset)
        assertEquals('b', cursor.peek(0))
        assertEquals('c', cursor.peek(1))
        assertNull(cursor.peek(2))
    }

    @Test
    fun `advance consumes elements sequentially and updates currentOffset`() {
        val cursor = sequenceOf(1, 2, 3).asCursor()

        assertEquals(0, cursor.currentOffset)
        assertEquals(1, cursor.advance())
        assertEquals(1, cursor.currentOffset)
        assertEquals(2, cursor.advance())
        assertEquals(2, cursor.currentOffset)
        assertEquals(3, cursor.advance())
        assertEquals(3, cursor.currentOffset)
        assertNull(cursor.advance())
        assertEquals(3, cursor.currentOffset)
        assertFalse(cursor.hasMore())
    }

    @Test
    fun `negative offset returns null safely without throwing exceptions`() {
        val cursor = sequenceOf('x', 'y').asCursor()

        assertNull(cursor.peek(-1))
        assertNull(cursor.peek(-100))
        assertEquals('x', cursor.peek(0))
        assertEquals(0, cursor.currentOffset)
    }

    @Test
    fun `consume extracts exact number of characters`() {
        val cursor = "hello world".asSequence().asCursor()

        assertEquals("hello", cursor.consume(5))
        assertEquals(5, cursor.currentOffset)
        assertEquals(' ', cursor.peek())
        assertEquals(" world", cursor.consume(10))
        assertEquals(11, cursor.currentOffset)
        assertFalse(cursor.hasMore())
    }
}
