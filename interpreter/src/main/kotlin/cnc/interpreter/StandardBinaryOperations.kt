package cnc.interpreter

object StandardBinaryOperations {
    
    private fun formatValue(value: Any?): String {
        if (value is Double && value % 1.0 == 0.0) {
            return value.toInt().toString()
        }
        return value?.toString() ?: "null"
    }

    fun add(left: Any, right: Any): Any {
        return if (left is String || right is String) {
            "${formatValue(left)}${formatValue(right)}"
        } else {
            NumberOperations.add(left, right)
        }
    }

    fun subtract(left: Any, right: Any): Any = NumberOperations.subtract(left, right)
    
    fun multiply(left: Any, right: Any): Any = NumberOperations.multiply(left, right)
    
    fun divide(left: Any, right: Any): Any = NumberOperations.divide(left, right)
}
