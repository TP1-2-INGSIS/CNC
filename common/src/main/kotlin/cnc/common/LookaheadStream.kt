package cnc.common

/**
 * Generic interface for streams with lookahead capability
 * and sequential consumption.
 */
interface LookaheadStream<T> {
    /**
     * Returns true if there are more elements available to consume.
     */
    fun hasMore(): Boolean

    /**
     * Inspects the element at the current position + [offset] without advancing the cursor.
     * [offset] 0 represents the next element to consume.
     * Returns null if the end of the stream is reached or if offset is negative.
     */
    fun peek(offset: Int = 0): T?

    /**
     * Consumes and returns the next element in the stream.
     * Returns null if there are no more elements.
     */
    fun advance(): T?
}

/**
 * Implementation of [LookaheadStream] backed by a Kotlin [Sequence]
 * and an O(1) circular buffer using [ArrayDeque].
 */
class SequenceLookaheadStream<T>(elements: Sequence<T>) : LookaheadStream<T> {
    private val iterator = elements.iterator()
    private val buffer = ArrayDeque<T>()

    override fun hasMore(): Boolean = buffer.isNotEmpty() || iterator.hasNext()

    override fun peek(offset: Int): T? {
        if (offset < 0) return null
        while (buffer.size <= offset && iterator.hasNext()) {
            buffer.addLast(iterator.next())
        }
        return buffer.getOrNull(offset)
    }

    override fun advance(): T? {
        return if (buffer.isNotEmpty()) buffer.removeFirst()
        else if (iterator.hasNext()) iterator.next()
        else null
    }
}

/**
 * Converts any [Sequence] into a [LookaheadStream].
 */
fun <T> Sequence<T>.asLookaheadStream(): LookaheadStream<T> = SequenceLookaheadStream(this)

/**
 * Converts any [Iterable] into a [LookaheadStream].
 */
fun <T> Iterable<T>.asLookaheadStream(): LookaheadStream<T> = this.asSequence().asLookaheadStream()
