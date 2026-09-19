package cnc.common

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Test
import java.io.File

class ContentManagerTest {

    @Test
    fun `openStream streams characters lazily and tracks lines incrementally`() {
        val content = StringContent("val x = 1;\nval y = 2;")
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

    @Test
    fun `inputStreamContent and companion factory stream content correctly`() {
        val stream = "let a: number = 42;".byteInputStream()
        val content = ContentManager.of(stream)
        val cursor = content.openStream()

        assertEquals('l', cursor.peek())
        assertEquals("let a: number = 42;", cursor.consume(19))
        assertFalse(cursor.hasMore())

        val strContent = ContentManager.of("hello")
        assertEquals('h', strContent.openStream().peek())

        val tempFile = File.createTempFile("test_of_file", ".prs")
        tempFile.writeText("content")
        try {
            val fileContent = ContentManager.of(tempFile)
            assertEquals('c', fileContent.openStream().peek())
        } finally {
            tempFile.delete()
        }

        org.junit.jupiter.api.assertThrows<IllegalArgumentException> {
            FileContent("non_existent_file_12345.prs")
        }
    }
}
