package cnc.linter

import cnc.ast.GenericStatement

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
    private val conventionName: String,
    private val validator: NamingValidator
) : LinterRule {

    override fun check(statement: GenericStatement): List<String> {
        if (statement.tag != targetTag) return emptyList()

        if (!statement.fields.has("name")) return emptyList()
        val varName = statement.fields.text("name")

        if (!validator.isValid(varName)) {
            return listOf("La variable '$varName' debería estar en formato $conventionName.")
        }

        return emptyList()
    }
}
