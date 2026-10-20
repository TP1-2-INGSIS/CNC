package cnc.printscript

import cnc.language.LanguageFacade

/**
 * Official entry point for building a PrintScript [LanguageFacade].
 *
 * Prefer the strongly-typed presets ([v1_0], [v1_1]) or the [PrintScriptVersion]
 * overload of [from]: they are verified at compile time and discoverable through
 * IDE autocompletion. The [String] overload of [from] exists only as a dynamic
 * fallback for entry points that receive the version as text (e.g. CLI flags).
 */
object PrintScriptEngine {
    /** Type-safe preset for PrintScript 1.0. */
    @Suppress("FunctionNaming", "ktlint:standard:function-naming")
    fun v1_0(): LanguageFacade = PrintScriptFacade(PrintScriptVersion.V1_0)

    /** Type-safe preset for PrintScript 1.1. */
    @Suppress("FunctionNaming", "ktlint:standard:function-naming")
    fun v1_1(): LanguageFacade = PrintScriptFacade(PrintScriptVersion.V1_1)

    /** Builds a facade from a strongly-typed [version]. */
    fun from(version: PrintScriptVersion): LanguageFacade = PrintScriptFacade(version)

    /**
     * Builds a facade from a raw [version] string (e.g. a CLI flag).
     * Throws [UnsupportedVersionException] if the text is not a known version.
     */
    fun from(version: String): LanguageFacade = from(PrintScriptVersion.from(version))
}
