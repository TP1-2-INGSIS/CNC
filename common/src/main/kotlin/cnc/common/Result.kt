package cnc.common

enum class ErrorType {
  LEXER,
  PARSER,
  CLI,
}

sealed interface Result<T> {
  val msg: String

  fun isOk(): Boolean
}

data class Success<T>(
  override val msg: String,
  val data: T,
) : Result<T> {
  override fun isOk(): Boolean = true
}

data class Failure<T>(
  override val msg: String,
  val type: ErrorType,
) : Result<T> {
  override fun isOk(): Boolean = false
}

inline fun <T, R> Result<T>.map(transform: (T) -> R): Result<R> =
  when (this) {
    is Success -> Success(msg, transform(data))
    is Failure -> Failure(msg, type)
  }

inline fun <T, R> Result<T>.flatMap(transform: (T) -> Result<R>): Result<R> =
  when (this) {
    is Success -> transform(data)
    is Failure -> Failure(msg, type)
  }

inline fun <T> Result<T>.onSuccess(action: (Success<T>) -> Unit): Result<T> {
  if (this is Success) action(this)
  return this
}

inline fun <T> Result<T>.onFailure(action: (Failure<T>) -> Unit): Result<T> {
  if (this is Failure) action(this)
  return this
}
