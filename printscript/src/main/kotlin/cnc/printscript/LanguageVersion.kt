package cnc.printscript

class UnsupportedVersionException(
    version: String,
) : IllegalArgumentException("Unsupported PrintScript language version: '$version'")

enum class LanguageVersion(
    val label: String,
) {
    V1_0("1.0"),
    V1_1("1.1"),
    ;

    companion object {
        fun from(raw: String): LanguageVersion {
            val normalized = raw.trim()
            return entries.firstOrNull { it.label == normalized }
                ?: throw UnsupportedVersionException(raw)
        }
    }
}
