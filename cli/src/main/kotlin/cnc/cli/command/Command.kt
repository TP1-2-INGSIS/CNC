package cnc.cli.command

import cnc.cli.args.ArgsContainer
import cnc.common.ErrorType
import cnc.common.Failure
import cnc.common.Result
import cnc.common.Success

interface Command {
    val tag: String
    fun execute(params: ArgsContainer): Result<Unit>
}

interface WrappedCommand : Command {
    val wrapped: Command
    override val tag: String get() = wrapped.tag
}

class HelpAttribute(
    override val wrapped: Command,
    val description: String,
    val usage: String = wrapped.tag,
    val paramHelp: Map<String, String> = emptyMap()
) : WrappedCommand {

    override fun execute(params: ArgsContainer): Result<Unit> {
        if (params.hasFlag("help") || params.hasFlag("h")) {
            return Success(getHelpText(), Unit)
        }
        return wrapped.execute(params)
    }

    fun getHelpText(): String {
        val sb = StringBuilder()
        sb.appendLine("Command: $tag")
        sb.appendLine("Description: $description")
        sb.appendLine("Usage: $usage")
        if (paramHelp.isNotEmpty()) {
            sb.appendLine("\nOptions & Flags:")
            paramHelp.forEach { (param, desc) ->
                sb.appendLine("  ${param.padEnd(20)} $desc")
            }
        }
        return sb.toString().trimEnd()
    }
}
