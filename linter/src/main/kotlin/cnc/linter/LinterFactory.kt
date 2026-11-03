package cnc.linter

import cnc.common.NamingValidator
import cnc.common.Result

@Deprecated(
    "Usar LinterBuilder.default().addValidator(...).build(json).",
    ReplaceWith("LinterBuilder.default().build(jsonString)"),
)
object LinterFactory {
    /** Construye un linter registrando los [validators] dados y delegando en [LinterBuilder]. */
    fun build(
        jsonString: String,
        validators: Map<String, NamingValidator>,
    ): Result<CNCLinter> {
        val builder =
            validators.entries.fold(LinterBuilder.empty()) { acc, (key, validator) ->
                acc.addValidator(key, validator)
            }
        return builder.build(jsonString)
    }
}
