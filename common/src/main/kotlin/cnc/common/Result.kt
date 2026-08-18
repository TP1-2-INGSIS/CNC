package cnc.common

enum class ErrorType {
  LEXER,
  PARSER,
  CLI
}

interface Result<T> {
  fun isOk() : Boolean
}

data class Success<T>(
  val msg: String,
  val data: T
) : Result<T> {
  override fun isOk() : Boolean = true
}

data class Failure<T>(
  val msg: String,
  val type: ErrorType
) : Result<T> {
  override fun isOk() : Boolean = false
}
