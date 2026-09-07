package cnc.linter

import cnc.ast.GenericStatement

enum class NamingConvention {
    CAMEL_CASE,
    SNAKE_CASE
}

interface NamingValidator {
    fun isValid(name: String): Boolean
}

class CamelCaseValidator : NamingValidator {
    override fun isValid(name: String) = name.matches(Regex("^[a-z]+([A-Z][a-z0-9]+)*$"))
}

class SnakeCaseValidator : NamingValidator {
    override fun isValid(name: String) = name.matches(Regex("^[a-z]+(_[a-z0-9]+)*$"))
}

class NamingConventionRule(
    private val targetTag: String,
    private val convention: NamingConvention
) : LinterRule {

    // Este mapita no se si deberia estar aca o para que el cliente lo configure, total se puede mover
    private val validators = mapOf(
        NamingConvention.CAMEL_CASE to CamelCaseValidator(),
        NamingConvention.SNAKE_CASE to SnakeCaseValidator()
    )

    override fun check(statement: GenericStatement): List<String> {
        if (statement.tag != targetTag) return emptyList()

        if (!statement.fields.has("name")) return emptyList()
        val varName = statement.fields.text("name")

        val validator = validators[convention]
            ?: return emptyList()

        if (!validator.isValid(varName)) {
            return listOf("La variable '$varName' debería estar en formato ${convention.name}.")
        }

        return emptyList()
    }
}
