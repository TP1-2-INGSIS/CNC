package cnc.language

/**
 * Identifies a version of a language. Each language provides its own
 * implementation (typically an enum) with the supported versions.
 */
interface LanguageVersion {
    val label: String
}
