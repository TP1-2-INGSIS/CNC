package cnc.common

enum class LanguageVersion(val label: String) {
    V1_0("1.0"),
    V1_1("1.1");

    companion object {
        fun fromString(version: String): LanguageVersion = when (version.trim().lowercase()) {
            "1.0", "v1.0", "v1_0" -> V1_0
            "1.1", "v1.1", "v1_1" -> V1_1
            else -> throw IllegalArgumentException("Unsupported PrintScript version: '$version'. Supported versions: '1.0', '1.1'")
        }
    }
}
