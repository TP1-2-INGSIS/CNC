package cnc.interpreter

import cnc.common.ErrorType
import cnc.common.Failure
import cnc.common.Result
import cnc.common.Success

object NumberOperations {

    private fun toDouble(value: Any): Double? = when (value) {
        is Number -> value.toDouble()
        else -> null
    }

    fun add(left: Any, right: Any): Result<Any> {
        val l = toDouble(left) ?: return Failure("Expected a number, but got ${left::class.simpleName}", ErrorType.RUNTIME)
        val r = toDouble(right) ?: return Failure("Expected a number, but got ${right::class.simpleName}", ErrorType.RUNTIME)
        return Success("ok", ValueFormatter.formatNumber(l + r))
    }

    fun subtract(left: Any, right: Any): Result<Any> {
        val l = toDouble(left) ?: return Failure("Expected a number, but got ${left::class.simpleName}", ErrorType.RUNTIME)
        val r = toDouble(right) ?: return Failure("Expected a number, but got ${right::class.simpleName}", ErrorType.RUNTIME)
        return Success("ok", ValueFormatter.formatNumber(l - r))
    }

    fun multiply(left: Any, right: Any): Result<Any> {
        val l = toDouble(left) ?: return Failure("Expected a number, but got ${left::class.simpleName}", ErrorType.RUNTIME)
        val r = toDouble(right) ?: return Failure("Expected a number, but got ${right::class.simpleName}", ErrorType.RUNTIME)
        return Success("ok", ValueFormatter.formatNumber(l * r))
    }

    fun divide(left: Any, right: Any): Result<Any> {
        val l = toDouble(left) ?: return Failure("Expected a number, but got ${left::class.simpleName}", ErrorType.RUNTIME)
        val r = toDouble(right) ?: return Failure("Expected a number, but got ${right::class.simpleName}", ErrorType.RUNTIME)
        if (r == 0.0) return Failure("Division by zero", ErrorType.RUNTIME)
        return Success("ok", ValueFormatter.formatNumber(l / r))
    }
}
