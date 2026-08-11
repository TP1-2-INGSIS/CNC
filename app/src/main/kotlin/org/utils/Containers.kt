package org.utils

interface Container<T> {
  fun add(value: T) : Result<Unit>;
  fun addBulk(value: List<T>) : Result<Unit>;
  fun remove(id: T) : Result<Unit>;
  fun removeBulk(id: List<T>) : Result<Unit>;

  fun contians(to_search : T) : Boolean;

  fun asSequence() : Result<Sequence<T>>;
}
