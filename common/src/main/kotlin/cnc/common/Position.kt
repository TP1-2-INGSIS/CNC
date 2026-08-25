package cnc.common

data class Position(
  val row: Int,
  val col: Int
)

fun Position.advance(char: Char): Position = when (char) {
  '\n' -> Position(row + 1, 0)
  else -> Position(row, col + 1)
}

