package cnc.semantic

import cnc.common.ErrorType
import cnc.common.Failure
import cnc.common.Result
import cnc.common.Success

fun interface BinaryOpResolver {
    fun resolve(leftType: String, rightType: String): Result<String>
}

fun interface UnaryOpResolver {
    fun resolve(operandType: String): Result<String>
}

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

    fun unaryNumeric(op: String) = UnaryOpResolver { operandType ->
        if (operandType == "number")
            Success("ok", "number")
        else
            Failure("Operador unario '$op' requiere 'number', pero se obtuvo '$operandType'", ErrorType.SEMANTIC)
    }

    val booleanNot = UnaryOpResolver { operandType ->
        if (operandType == "boolean")
            Success("ok", "boolean")
        else
            Failure("Operador unario '!' requiere 'boolean', pero se obtuvo '$operandType'", ErrorType.SEMANTIC)
    }
}
