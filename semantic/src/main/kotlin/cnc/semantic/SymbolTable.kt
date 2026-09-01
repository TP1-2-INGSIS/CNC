package cnc.semantic

class SymbolTable(
    private val validTypes: Set<String>
) {
    private val variables = mutableMapOf<String, String>()

    fun isValidType(type: String): Boolean = type in validTypes
    fun isDeclared(name: String): Boolean = name in variables
    fun declare(name: String, type: String) { variables[name] = type }
    fun typeOf(name: String): String? = variables[name]

    // Vista de solo lectura para ExpressionTypeVisitor
    fun asReadOnly(): Map<String, String> = variables
}
