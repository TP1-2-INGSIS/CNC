package cnc.interpreter

import cnc.common.Result
import cnc.common.Success

object StandardBinaryOperations {

    fun add(left: Any, right: Any): Result<Any> {
        return if (left is String || right is String) {
            Success("ok", "${ValueFormatter.format(left)}${ValueFormatter.format(right)}")
        } else {
            NumberOperations.add(left, right)
        }
    }

    fun subtract(left: Any, right: Any): Result<Any> = NumberOperations.subtract(left, right)

    fun multiply(left: Any, right: Any): Result<Any> = NumberOperations.multiply(left, right)

    fun divide(left: Any, right: Any): Result<Any> = NumberOperations.divide(left, right)
}
