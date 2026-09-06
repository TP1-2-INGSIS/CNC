package cnc.common

/**
 * Maps 0-indexed character offsets to 2D [Position] (row, col) in O(log L) time
 * using an incremental sorted list of line-start offsets and Kotlin's built-in binarySearch.
 */
class LineIndex {
    private val lineStarts = mutableListOf(0)

    /**
     * Records the starting character [offset] of a new line.
     */
    fun recordLineStart(offset: Int) {
        lineStarts.add(offset)
    }

    /**
     * Resolves the 2D [Position] (0-indexed row and col) for a given character [offset].
     */
    fun positionOf(offset: Int): Position {
        if (lineStarts.isEmpty()) return Position(0, 0)
        val search = lineStarts.binarySearch(offset)
        val line = if (search >= 0) search else -search - 2
        val validLine = line.coerceAtLeast(0)
        val col = offset - lineStarts[validLine]
        return Position(row = validLine, col = col)
    }

    companion object {
        /**
         * Builds a [LineIndex] from a static [CharSequence].
         */
        fun build(source: CharSequence): LineIndex = LineIndex().apply {
            for (i in 0 until source.length) {
                if (source[i] == '\n') {
                    recordLineStart(i + 1)
                }
            }
        }
    }
}
