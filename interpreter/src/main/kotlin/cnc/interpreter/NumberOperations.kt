package cnc.interpreter

object NumberOperations {

    private fun toDouble(value: Any): Double = when (value) {
        is Number -> value.toDouble()
        else -> throw RuntimeException("Expected a number, but got ${value::class.simpleName}")
    }

    private fun formatResult(result: Double): Number {
        return if (result % 1.0 == 0.0) {
            result.toInt()
        } else {
            result
        }
    }

    fun add(left: Any, right: Any): Number {
        return formatResult(toDouble(left) + toDouble(right))
    }

    fun subtract(left: Any, right: Any): Number {
        return formatResult(toDouble(left) - toDouble(right))
    }

    fun multiply(left: Any, right: Any): Number {
        return formatResult(toDouble(left) * toDouble(right))
    }

    fun divide(left: Any, right: Any): Number {
        val r = toDouble(right)
        if (r == 0.0) throw ArithmeticException("Division by zero")
        return formatResult(toDouble(left) / r)
    }
}
