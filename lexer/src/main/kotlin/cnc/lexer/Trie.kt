package cnc.lexer

data class TrieNode<T>(
    val value: T? = null,
    val children: Map<Char, TrieNode<T>> = emptyMap()
)

fun <T> TrieNode<T>.matchLongest(stream: CharStream, offset: Int = 0): Pair<T, Int>? {
    val char = stream.peek(offset)
    if (char != null) {
        val deeperMatch = children[char]?.matchLongest(stream, offset + 1)
        if (deeperMatch != null) return deeperMatch
    }

    val terminalValue = value ?: return null
    return terminalValue to offset
}

fun <T> TrieNode<T>.matchExact(text: CharSequence, index: Int = 0): T? {
    if (index == text.length) return value
    val child = children[text[index]] ?: return null
    return child.matchExact(text, index + 1)
}

fun <T> buildTrie(entries: Iterable<Pair<String, T>>): TrieNode<T> {
    var root = TrieNode<T>()
    for ((key, value) in entries) {
        root = insert(root, key, value, 0)
    }
    return root
}

fun <T> buildTrie(entries: Map<String, T>): TrieNode<T> = buildTrie(entries.toList())

private fun <T> insert(node: TrieNode<T>, key: String, value: T, index: Int): TrieNode<T> {
    if (index == key.length) {
        return node.copy(value = value)
    }
    val char = key[index]
    val child = node.children[char] ?: TrieNode()
    val updatedChild = insert(child, key, value, index + 1)
    return node.copy(children = node.children + (char to updatedChild))
}
