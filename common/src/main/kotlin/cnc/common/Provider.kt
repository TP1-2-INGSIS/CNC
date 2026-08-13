package cnc.common

interface Provider<T, S> {
  fun getTypes() : Set<T>
  fun getValue(type: T) : S?
}
