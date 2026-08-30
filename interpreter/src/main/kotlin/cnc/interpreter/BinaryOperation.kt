package cnc.interpreter

fun interface BinaryOperation {
    fun execute(left: Any, right: Any): Any
}
