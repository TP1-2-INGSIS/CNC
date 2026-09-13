package cnc.common

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Test
import java.io.File

class ContentManagerTest {

    @Test
    fun `openStream streams characters lazily and tracks lines incrementally`() {
        val content = StrContent("val x = 1;\nval y = 2;")
        val cursor = content.openStream(bufferSize = 4) // small buffer to test multiple chunk reads

        assertEquals('v', cursor.peek())
        assertEquals(Position(0, 0), cursor.currentPosition)
        assertEquals("val x = 1;\n", cursor.consume(11))
        assertEquals(Position(1, 0), cursor.currentPosition)

        assertEquals("val y = 2;", cursor.consume(10))
        assertEquals(Position(1, 10), cursor.currentPosition)
        assertFalse(cursor.hasMore())
    }

    @Test
    fun `fileContent validates file existence and streams content`() {
        val tempFile = File.createTempFile("test_cnc", ".prs")
        tempFile.writeText("let greeting = \"hello\";\nprintln(greeting);")
        try {
            val content = FileContent(tempFile.absolutePath)
            val cursor = content.openStream(bufferSize = 8)

            assertEquals("let greeting = \"hello\";\n", cursor.consume(24))
            assertEquals(Position(1, 0), cursor.currentPosition)
        } finally {
            tempFile.delete()
        }
    }
}
