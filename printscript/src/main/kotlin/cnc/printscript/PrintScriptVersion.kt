package cnc.printscript

import cnc.language.LanguageVersion

class UnsupportedVersionException(
    version: String,
) : IllegalArgumentException("Unsupported PrintScript language version: '$version'")

enum class PrintScriptVersion(
    override val label: String,
) : LanguageVersion {
    V1_0("1.0"),
    V1_1("1.1"),
    ;

    companion object {
        fun from(raw: String): PrintScriptVersion {
            val normalized = raw.trim()
            return entries.firstOrNull { it.label == normalized }
                ?: throw UnsupportedVersionException(raw)
        }
    }
}
