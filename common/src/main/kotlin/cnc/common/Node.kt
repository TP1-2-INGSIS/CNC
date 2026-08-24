package cnc.common

// TODO: Mejor hacerlo como lista.
data class Node<T>(
  val right: Node<T>,
  val left: Node<T>,
)
