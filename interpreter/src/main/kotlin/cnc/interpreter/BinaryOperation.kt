package cnc.interpreter

import cnc.common.Result

fun interface BinaryOperation {
    fun execute(left: Any, right: Any): Result<Any>
}
