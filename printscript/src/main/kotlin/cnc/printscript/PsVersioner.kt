package cnc.printscript

import cnc.language.LanguageFacade

@Deprecated(
    message =
        "Use PrintScriptEngine instead: prefer the type-safe presets (v1_0/v1_1) " +
            "or PrintScriptEngine.from(version). This dynamic String entry point remains only for CLI usage.",
    replaceWith =
        ReplaceWith(
            "PrintScriptEngine.from(version)",
            "cnc.printscript.PrintScriptEngine",
        ),
)
object PsVersioner {
    fun version(version: String): LanguageFacade = PrintScriptEngine.from(version)
}
