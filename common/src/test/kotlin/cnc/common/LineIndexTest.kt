package cnc.common

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class LineIndexTest {

    @Test
    fun `single line positions calculated accurately`() {
        val index = LineIndex.build("let a = 10;")

        assertEquals(Position(0, 0), index.positionOf(0))
        assertEquals(Position(0, 4), index.positionOf(4))
        assertEquals(Position(0, 10), index.positionOf(10))
    }

    @Test
    fun `multiline positions calculated across newlines`() {
        // "ab\ncd\ne"
        // Line 0: "ab\n" (offsets 0, 1, 2)
        // Line 1: "cd\n" (offsets 3, 4, 5)
        // Line 2: "e"    (offset 6)
        val index = LineIndex.build("ab\ncd\ne")

        assertEquals(Position(0, 0), index.positionOf(0)) // 'a'
        assertEquals(Position(0, 1), index.positionOf(1)) // 'b'
        assertEquals(Position(0, 2), index.positionOf(2)) // '\n'
        assertEquals(Position(1, 0), index.positionOf(3)) // 'c'
        assertEquals(Position(1, 1), index.positionOf(4)) // 'd'
        assertEquals(Position(1, 2), index.positionOf(5)) // '\n'
        assertEquals(Position(2, 0), index.positionOf(6)) // 'e'
    }

    @Test
    fun `incremental recordLineStart matches static build`() {
        val staticIndex = LineIndex.build("foo\nbar\nbaz")

        val incrementalIndex = LineIndex()
        // 'f'(0), 'o'(1), 'o'(2), '\n'(3) -> next line at 4
        incrementalIndex.recordLineStart(4)
        // 'b'(4), 'a'(5), 'r'(6), '\n'(7) -> next line at 8
        incrementalIndex.recordLineStart(8)

        for (offset in 0..10) {
            assertEquals(staticIndex.positionOf(offset), incrementalIndex.positionOf(offset))
        }
    }
}
