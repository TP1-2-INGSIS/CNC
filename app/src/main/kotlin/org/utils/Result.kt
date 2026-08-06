package org.utils

enum class ErrorType {
  LEXER,
  PARSER
}

interface Result<T> {
  fun isOk() : Boolean
}

data class Success<T>(
  val data: T,
  val msg: String
) : Result<T> {
  override fun isOk() : Boolean = true
}

data class Failure<T>(
  val msg: String,
  val type: ErrorType
) : Result<T> {
  override fun isOk() : Boolean = false
}
