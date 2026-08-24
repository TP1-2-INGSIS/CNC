package cnc.cli

import cnc.cli.args.ArgsManager
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class ArgsManagerTest {
  @Test
  fun `test tokenize handles simple spaces`() {
    val input = "gcnc --file=main.cnc --verbose"
    val tokens = ArgsManager.tokenize(input)
    assertEquals(listOf("gcnc", "--file=main.cnc", "--verbose"), tokens)
  }

  @Test
  fun `test tokenize preserves double quoted values with spaces`() {
    val input = "gcnc --file=\"path to my/source file.cnc\" --verbose"
    val tokens = ArgsManager.tokenize(input)
    assertEquals(listOf("gcnc", "--file=path to my/source file.cnc", "--verbose"), tokens)
  }

  @Test
  fun `test tokenize preserves single quoted values with spaces`() {
    val input = "gcnc 'my positional arg' --out='build/out.bin'"
    val tokens = ArgsManager.tokenize(input)
    assertEquals(listOf("gcnc", "my positional arg", "--out=build/out.bin"), tokens)
  }

  @Test
  fun `test getArgsContainer parses options, flags, and positional args`() {
    val tokens = listOf("--file=test.cnc", "--verbose", "-check", "extra_arg")
    val container = ArgsManager.getArgsContainer(tokens)

    assertTrue(container.hasOption("file"))
    assertTrue(container.hasOption("--file"))
    assertEquals("test.cnc", container.getOption("file"))
    assertEquals("test.cnc", container.getOption("--file"))

    assertTrue(container.hasFlag("verbose"))
    assertTrue(container.hasFlag("--verbose"))
    assertTrue(container.hasFlag("check"))
    assertTrue(container.hasFlag("-check"))
    assertFalse(container.hasFlag("missing"))

    assertEquals("extra_arg", container.getPositional(0))
    assertNull(container.getPositional(1))
    assertTrue(container.hasNPositional(1))
    assertTrue(container.hasAtLeastPositional(1))
  }

  @Test
  fun `test getArgsContainer unquotes option values`() {
    val tokens = listOf("--file=\"quoted/path.cnc\"", "--title='my app'")
    val container = ArgsManager.getArgsContainer(tokens)

    assertEquals("quoted/path.cnc", container.getOption("file"))
    assertEquals("my app", container.getOption("title"))
  }
}
