package cnc.interpreter

import cnc.common.Result

fun interface UnaryOperation {
    fun execute(operand: Any): Result<Any>
}
