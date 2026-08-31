package cnc.semantic

import cnc.common.ErrorType
import cnc.common.Failure
import cnc.common.Result
import cnc.common.Success

fun interface BinaryOpResolver {
    fun resolve(leftType: String, rightType: String): Result<String>
} // Basicamente recibe dos tipos y 'resuelve' que tipo corresponde. Distinto que el de interpreter que hace operaciones

object TypeResolvers {

    fun numericOnly(op: String) = BinaryOpResolver { left, right ->
        if (left == "number" && right == "number")
            Success("ok", "number")
        else
            Failure("Operador '$op' requiere 'number', se recibió '$left' y '$right'", ErrorType.SEMANTIC)
    }

    val additionOrConcat = BinaryOpResolver { left, right ->
        when {
            left == "number" && right == "number" -> Success("ok", "number")
            left == "string" || right == "string"  -> Success("ok", "string")
            else -> Failure("Operador '+' incompatible entre '$left' y '$right'", ErrorType.SEMANTIC)
        }
    }
}
