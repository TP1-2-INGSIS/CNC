package cnc.common

/** Valida un valor de tipo [T]. */
fun interface Validator<T> {
    fun isValid(value: T): Boolean
}

/** Validador de nombres: un [Validator] sobre `String`. */
fun interface NamingValidator : Validator<String>
