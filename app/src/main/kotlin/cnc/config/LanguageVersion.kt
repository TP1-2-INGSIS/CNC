package cnc.config

/**
 * Exception thrown when a requested language version is not supported by this
 * PrintScript build.
 */
class UnsupportedVersionException(version: String) :
    IllegalArgumentException("Unsupported PrintScript language version: '$version'")

/**
 * Supported PrintScript language versions.
 *
 * These represent the language specification (features) — NOT the Maven/package
 * release version, which is resolved independently via repository tags.
 *
 * The canonical string form is `"x.x"` (e.g. `"1.0"`, `"1.1"`), which is the
 * format the TCK passes at runtime.
 */
enum class LanguageVersion(val label: String) {
    V1_0("1.0"),
    V1_1("1.1");

    companion object {
        /**
         * Parses a language version from its `"x.x"` string form.
         *
         * @throws UnsupportedVersionException if the string does not match a
         * supported version.
         */
        fun from(raw: String): LanguageVersion {
            val normalized = raw.trim()
            return entries.firstOrNull { it.label == normalized }
                ?: throw UnsupportedVersionException(raw)
        }
    }
}
