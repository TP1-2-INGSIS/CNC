package cnc.linter

import cnc.common.NamingValidator

/** Registro inmutable de [NamingValidator] que normaliza la clave de entrada antes de resolver. */
class ValidatorsContainer private constructor(
    private val byKey: Map<String, NamingValidator>,
    private val normalizer: (String) -> String,
) {
    /** Devuelve una copia con [validator] registrado bajo [key]. */
    fun withValidator(
        key: String,
        validator: NamingValidator,
    ): ValidatorsContainer = ValidatorsContainer(byKey + (key to validator), normalizer)

    /** Resuelve el validador para [rawKey] tras normalizarla, o `null` si no hay ninguno. */
    fun resolve(rawKey: String): NamingValidator? = byKey[normalizer(rawKey)] ?: byKey[rawKey]

    companion object {
        /** Normalizador por defecto: colapsa cualquier variante de camel/snake a su clave canónica. */
        private val defaultNormalizer: (String) -> String = { key ->
            when {
                key.contains("camel", ignoreCase = true) -> "camelCase"
                key.contains("snake", ignoreCase = true) -> "snake_case"
                else -> key.trim()
            }
        }

        /** Container vacío con el normalizador por defecto. */
        fun empty(normalizer: (String) -> String = defaultNormalizer): ValidatorsContainer =
            ValidatorsContainer(emptyMap(), normalizer)

        /** Container con los validadores estándar (camelCase, snake_case) ya registrados. */
        fun default(): ValidatorsContainer =
            empty()
                .withValidator("camelCase", CamelCaseValidator())
                .withValidator("snake_case", SnakeCaseValidator())
    }
}
