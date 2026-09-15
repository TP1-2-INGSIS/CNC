package cnc.common

// TODO: Mejor hacerlo como lista.
data class Node<T> (
  val right: Node<T>? = null,
  val left: Node<T>? = null
)
