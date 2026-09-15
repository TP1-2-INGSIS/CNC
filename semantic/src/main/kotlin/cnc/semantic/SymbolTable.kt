package cnc.semantic

import cnc.common.ErrorType
import cnc.common.Failure
import cnc.common.Result
import cnc.common.Success

data class Symbol(
    val type: String,
    val isMutable: Boolean
)

class SymbolTable(
    val validTypes: Set<String>,
    private val parent: SymbolTable? = null
) {
    private val symbols = mutableMapOf<String, Symbol>()

    fun isValidType(type: String): Boolean = type in validTypes

    fun isDeclaredInCurrentScope(name: String): Boolean = symbols.containsKey(name)

    fun isDeclared(name: String): Boolean = symbols.containsKey(name) || (parent?.isDeclared(name) == true)

    fun declare(name: String, type: String, isMutable: Boolean = true): Result<Unit> {
        if (!isValidType(type)) {
            return Failure("Tipo '$type' no reconocido", ErrorType.SEMANTIC)
        }
        if (isDeclaredInCurrentScope(name)) {
            return Failure("Variable '$name' ya fue declarada en este ámbito", ErrorType.SEMANTIC)
        }
        symbols[name] = Symbol(type, isMutable)
        return Success("ok", Unit)
    }

    fun lookup(name: String): Symbol? = symbols[name] ?: parent?.lookup(name)

    fun typeOf(name: String): String? = lookup(name)?.type

    fun isMutable(name: String): Boolean? = lookup(name)?.isMutable

    fun createChild(): SymbolTable = SymbolTable(validTypes = validTypes, parent = this)

    // Vista de solo lectura para ExpressionTypeResolver
    fun asReadOnly(): Map<String, String> {
        val allVars = mutableMapOf<String, String>()
        parent?.asReadOnly()?.let { allVars.putAll(it) }
        symbols.forEach { (k, v) -> allVars[k] = v.type }
        return allVars
    }
}
