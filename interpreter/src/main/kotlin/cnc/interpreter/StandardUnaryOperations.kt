package cnc.interpreter

import cnc.common.Result

object StandardUnaryOperations {
    fun negate(operand: Any): Result<Any> = NumberOperations.negate(operand)

    fun positive(operand: Any): Result<Any> = NumberOperations.positive(operand)
}
