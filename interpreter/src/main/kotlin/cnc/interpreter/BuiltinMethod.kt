package cnc.interpreter

import cnc.common.BuiltinMethod
import cnc.common.BuiltinMethodBuilder
import cnc.common.ErrorType
import cnc.common.Failure
import cnc.common.Result
import cnc.common.Success

typealias BuiltinMethod = cnc.common.BuiltinMethod
typealias BuiltinMethodBuilder = cnc.common.BuiltinMethodBuilder

object BuiltinMethodFactory {

    fun builder(): BuiltinMethodBuilder = BuiltinMethodBuilder()

    fun println(output: (String) -> Unit = ::println): BuiltinMethod =
        builder()
            .name("println")
            .returnType(null)
            .execute { args ->
                output(args.joinToString(" ") { ValueFormatter.format(it) })
                Success("ok", Unit)
            }
            .build()

    fun readInput(input: (String) -> String = { readln() }): BuiltinMethod =
        builder()
            .name("readInput")
            .returnType("string")
            .execute { args ->
                val prompt = args.firstOrNull()?.let { ValueFormatter.format(it) } ?: ""
                Success("ok", input(prompt))
            }
            .build()

    fun readEnv(envProvider: (String) -> String? = System::getenv): BuiltinMethod =
        builder()
            .name("readEnv")
            .returnType("string")
            .execute { args ->
                val varName = args.firstOrNull()?.toString()
                    ?: return@execute Failure("Missing env variable name", ErrorType.RUNTIME)
                val value = envProvider(varName)
                    ?: return@execute Failure("Env variable '$varName' not found", ErrorType.RUNTIME)
                Success("ok", value)
            }
            .build()
}
