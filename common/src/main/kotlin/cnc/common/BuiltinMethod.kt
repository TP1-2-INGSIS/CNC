package cnc.common

interface BuiltinMethod {
    val name: String
    val returnType: String? get() = null
    fun execute(arguments: List<Any?>): Result<Any?>
}

class BuiltinMethodBuilder {
    private var name: String? = null
    private var returnType: String? = null
    private var executeFn: ((List<Any?>) -> Result<Any?>)? = null

    fun name(name: String): BuiltinMethodBuilder = apply { this.name = name }
    fun returnType(returnType: String?): BuiltinMethodBuilder = apply { this.returnType = returnType }
    fun execute(fn: (List<Any?>) -> Result<Any?>): BuiltinMethodBuilder = apply { this.executeFn = fn }

    fun build(): BuiltinMethod {
        val n = requireNotNull(name) { "BuiltinMethod requires a name" }
        val ret = returnType
        val exec = requireNotNull(executeFn) { "BuiltinMethod '$n' requires an execute function" }
        return object : BuiltinMethod {
            override val name: String = n
            override val returnType: String? = ret
            override fun execute(arguments: List<Any?>): Result<Any?> = exec(arguments)
        }
    }
}
