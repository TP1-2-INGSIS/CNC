package cnc.interpreter

object ValueFormatter {

    fun format(value: Any?): String {
        return when (value) {
            null -> "null"
            is Double -> if (value % 1.0 == 0.0) value.toLong().toString() else value.toString()
            is Float -> if (value % 1.0f == 0.0f) value.toLong().toString() else value.toString()
            is Number -> value.toString()
            is String -> value
            is Boolean -> value.toString()
            else -> value.toString()
        }
    }

    fun formatNumber(value: Double): Number {
        return if (value % 1.0 == 0.0) {
            value.toInt()
        } else {
            value
        }
    }
}
