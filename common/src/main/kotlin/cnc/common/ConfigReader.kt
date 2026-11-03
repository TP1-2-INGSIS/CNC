package cnc.common

/** Lee configuración por aliases (primera clave presente), agnóstico del parser JSON. */
class ConfigReader(
    private val values: Map<String, Any?>,
) {
    /** `true` si alguna de las [candidateKeys] existe y vale booleano `true`. */
    fun isEnabled(candidateKeys: List<String>): Boolean {
        val raw = firstPresent(candidateKeys) ?: return false
        return when (raw) {
            is Boolean -> raw
            is String -> raw.toBooleanStrictOrNull() ?: false
            else -> false
        }
    }

    /** Entero de la primera clave presente, o `null` si no hay ninguna o no es numérica. */
    fun readInt(candidateKeys: List<String>): Int? {
        val raw = firstPresent(candidateKeys) ?: return null
        return when (raw) {
            is Int -> raw
            is Number -> raw.toInt()
            is String -> raw.trim().toIntOrNull()
            else -> null
        }
    }

    /** String (trim) de la primera clave presente, o `null` si no hay ninguna. */
    fun readString(candidateKeys: List<String>): String? {
        val raw = firstPresent(candidateKeys) ?: return null
        return raw.toString().trim()
    }

    private fun firstPresent(candidateKeys: List<String>): Any? {
        val key = candidateKeys.firstOrNull { values.containsKey(it) && values[it] != null } ?: return null
        return values[key]
    }

    companion object {
        /** Reader vacío: toda consulta devuelve ausente. */
        val EMPTY = ConfigReader(emptyMap())

        fun fromMap(values: Map<String, Any?>): ConfigReader = ConfigReader(values)
    }
}
