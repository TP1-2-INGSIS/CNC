package cnc.printscript

import cnc.config.LanguageVersion

/**
 * Entry point for obtaining a version-specific [PrintScriptFacade].
 *
 * The language version (features) is selected at runtime via the `"x.x"` string
 * form (e.g. `"1.0"`, `"1.1"`), independently of the Maven/package release
 * version, which is resolved via repository tags.
 *
 * Usage:
 * ```
 * val ps = PsVersioner.version("1.0")
 * ps.lint(source, configJson)
 * ```
 */
object PsVersioner {

    /**
     * Returns the [PrintScriptFacade] for the given language version.
     *
     * @param version language version in `"x.x"` form (e.g. `"1.0"`, `"1.1"`).
     * @throws cnc.config.UnsupportedVersionException if the version is not supported.
     */
    fun version(version: String): PrintScriptFacade =
        PrintScriptEngine(LanguageVersion.from(version))
}
