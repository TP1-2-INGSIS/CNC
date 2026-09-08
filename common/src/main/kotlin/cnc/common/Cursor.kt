package cnc.common

/**
 * Generic cursor with lookahead and consumption tracking.
 */
interface Cursor<T> {
    /**
     * Offset representing the number of elements consumed so far (0-indexed).
     */
    val currentOffset: Int

    /**
     * Returns true if there are more elements available to consume.
     */
    fun hasMore(): Boolean

    /**
     * Inspects the element at [currentOffset] + [offset] without advancing the cursor.
     * [offset] 0 represents the next element to consume.
     * Returns null if the end of the stream is reached or if [offset] is negative.
     */
    fun peek(offset: Int = 0): T?

    /**
     * Consumes and returns the next element in the stream, incrementing [currentOffset].
     * Returns null if there are no more elements.
     */
    fun advance(): T?
}

/**
 * Implementation of [Cursor] backed by a Kotlin [Sequence]
 * and an O(1) buffer using [ArrayDeque] for lookahead.
 */
class SequenceCursor<T>(elements: Sequence<T>) : Cursor<T> {
    private val iterator = elements.iterator()
    private val buffer = ArrayDeque<T>()
    private var _offset = 0

    override val currentOffset: Int
        get() = _offset

    override fun hasMore(): Boolean = buffer.isNotEmpty() || iterator.hasNext()

    override fun peek(offset: Int): T? {
        if (offset < 0) return null
        while (buffer.size <= offset && iterator.hasNext()) {
            buffer.addLast(iterator.next())
        }
        return buffer.getOrNull(offset)
    }

    override fun advance(): T? {
        val item = when {
            buffer.isNotEmpty() -> buffer.removeFirst()
            iterator.hasNext() -> iterator.next()
            else -> null
        }
        if (item != null) {
            _offset++
        }
        return item
    }
}

/**
 * Specialized [Cursor] for characters that tracks 2D [Position] (row, col)
 * using zero-allocation primitive integer counters.
 */
interface CharCursor : Cursor<Char> {
    /**
     * The 2D [Position] (0-indexed row and col) of the *next* character to be consumed.
     */
    val currentPosition: Position
}

/**
 * Implementation of [CharCursor] backed by a Kotlin [Sequence] of characters
 * with lookahead and primitive line/column tracking without heap allocation on advance.
 */
class TrackingCharCursor(elements: Sequence<Char>) : CharCursor {
    private val iterator = elements.iterator()
    private val buffer = ArrayDeque<Char>()
    private var _offset = 0
    private var _line = 0
    private var _col = 0

    override val currentOffset: Int
        get() = _offset

    override val currentPosition: Position
        get() = Position(_line, _col)

    override fun hasMore(): Boolean = buffer.isNotEmpty() || iterator.hasNext()

    override fun peek(offset: Int): Char? {
        if (offset < 0) return null
        while (buffer.size <= offset && iterator.hasNext()) {
            buffer.addLast(iterator.next())
        }
        return buffer.getOrNull(offset)
    }

    override fun advance(): Char? {
        val item = when {
            buffer.isNotEmpty() -> buffer.removeFirst()
            iterator.hasNext() -> iterator.next()
            else -> null
        }
        if (item != null) {
            _offset++
            if (item == '\n') {
                _line++
                _col = 0
            } else {
                _col++
            }
        }
        return item
    }
}

/**
 * Converts any [Sequence] into a [Cursor].
 */
fun <T> Sequence<T>.asCursor(): Cursor<T> = SequenceCursor(this)

/**
 * Converts any [Iterable] into a [Cursor].
 */
fun <T> Iterable<T>.asCursor(): Cursor<T> = this.asSequence().asCursor()

/**
 * Converts a [Sequence] of [Char] into a [CharCursor] with position tracking.
 */
fun Sequence<Char>.asCharCursor(): CharCursor = TrackingCharCursor(this)

/**
 * Converts a [CharSequence] into a [CharCursor] with position tracking.
 */
fun CharSequence.asCharCursor(): CharCursor = this.asSequence().asCharCursor()

/**
 * Consumes [count] characters from the cursor and returns them as a [String].
 */
fun Cursor<Char>.consume(count: Int): String = buildString(count) {
    repeat(count) {
        append(advance() ?: return@buildString)
    }
}

