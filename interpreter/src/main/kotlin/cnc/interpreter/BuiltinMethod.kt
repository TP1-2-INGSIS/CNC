package cnc.interpreter

import cnc.common.ErrorType
import cnc.common.Failure
import cnc.common.Result
import cnc.common.Success

interface BuiltinMethod {
    val name: String
    fun execute(arguments: List<Any?>): Result<Any?>
}

class BuiltinMethodBuilder {
    private var name: String? = null
    private var executeFn: ((List<Any?>) -> Result<Any?>)? = null

    fun name(name: String): BuiltinMethodBuilder = apply { this.name = name }
    fun execute(fn: (List<Any?>) -> Result<Any?>): BuiltinMethodBuilder = apply { this.executeFn = fn }

    fun build(): BuiltinMethod {
        val n = requireNotNull(name) { "BuiltinMethod requires a name" }
        val exec = requireNotNull(executeFn) { "BuiltinMethod '$n' requires an execute function" }
        return object : BuiltinMethod {
            override val name: String = n
            override fun execute(arguments: List<Any?>): Result<Any?> = exec(arguments)
        }
    }
}

object BuiltinMethodFactory {

    fun builder(): BuiltinMethodBuilder = BuiltinMethodBuilder()

    fun println(output: (String) -> Unit = ::println): BuiltinMethod =
        builder()
            .name("println")
            .execute { args ->
                output(args.joinToString(" ") { ValueFormatter.format(it) })
                Success("ok", Unit)
            }
            .build()

    fun readInput(input: (String) -> String = { readln() }): BuiltinMethod =
        builder()
            .name("readInput")
            .execute { args ->
                val prompt = args.firstOrNull()?.let { ValueFormatter.format(it) } ?: ""
                Success("ok", input(prompt))
            }
            .build()

    fun readEnv(envProvider: (String) -> String? = System::getenv): BuiltinMethod =
        builder()
            .name("readEnv")
            .execute { args ->
                val varName = args.firstOrNull()?.toString()
                    ?: return@execute Failure("Missing env variable name", ErrorType.RUNTIME)
                val value = envProvider(varName)
                    ?: return@execute Failure("Env variable '$varName' not found", ErrorType.RUNTIME)
                Success("ok", value)
            }
            .build()
}
