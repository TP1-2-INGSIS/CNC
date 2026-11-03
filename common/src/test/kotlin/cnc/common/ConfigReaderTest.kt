package cnc.common

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class ConfigReaderTest {
    @Test
    fun `isEnabled returns first present boolean among aliases`() {
        val reader = ConfigReader.fromMap(mapOf("short-alias" to true))
        assertTrue(reader.isEnabled(listOf("canonical-key", "short-alias")))
    }

    @Test
    fun `isEnabled coerces string true and false`() {
        assertTrue(ConfigReader.fromMap(mapOf("k" to "true")).isEnabled(listOf("k")))
        assertFalse(ConfigReader.fromMap(mapOf("k" to "false")).isEnabled(listOf("k")))
        assertFalse(ConfigReader.fromMap(mapOf("k" to "notabool")).isEnabled(listOf("k")))
    }

    @Test
    fun `isEnabled returns false when absent or null or wrong type`() {
        assertFalse(ConfigReader.EMPTY.isEnabled(listOf("missing")))
        assertFalse(ConfigReader.fromMap(mapOf("k" to null)).isEnabled(listOf("k")))
        assertFalse(ConfigReader.fromMap(mapOf("k" to 123)).isEnabled(listOf("k")))
    }

    @Test
    fun `readInt handles Int, Number, and numeric String`() {
        assertEquals(4, ConfigReader.fromMap(mapOf("k" to 4)).readInt(listOf("k")))
        assertEquals(2, ConfigReader.fromMap(mapOf("k" to 2.9)).readInt(listOf("k")))
        assertEquals(7, ConfigReader.fromMap(mapOf("k" to " 7 ")).readInt(listOf("k")))
    }

    @Test
    fun `readInt returns null when absent or not numeric`() {
        assertNull(ConfigReader.EMPTY.readInt(listOf("missing")))
        assertNull(ConfigReader.fromMap(mapOf("k" to "abc")).readInt(listOf("k")))
        assertNull(ConfigReader.fromMap(mapOf("k" to true)).readInt(listOf("k")))
    }

    @Test
    fun `readString trims and falls back through aliases`() {
        val reader = ConfigReader.fromMap(mapOf("second" to "  camelCase  "))
        assertEquals("camelCase", reader.readString(listOf("first", "second")))
        assertNull(ConfigReader.EMPTY.readString(listOf("missing")))
    }

    @Test
    fun `first present alias wins over later ones`() {
        val reader = ConfigReader.fromMap(mapOf("a" to 1, "b" to 2))
        assertEquals(1, reader.readInt(listOf("a", "b")))
    }
}
