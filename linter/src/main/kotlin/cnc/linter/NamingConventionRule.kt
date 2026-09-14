package cnc.linter

import cnc.ast.Declaration
import cnc.ast.Statement

fun interface NamingValidator {
    fun isValid(name: String): Boolean
}

class CamelCaseValidator : NamingValidator {
    override fun isValid(name: String) = name.matches(Regex("^[a-z]+([A-Z][a-z0-9]+)*$"))
}

class SnakeCaseValidator : NamingValidator {
    override fun isValid(name: String) = name.matches(Regex("^[a-z]+(_[a-z0-9]+)*$"))
}

class NamingConventionRule(
    private val conventionName: String,
    private val validator: NamingValidator
) : LinterRule {

    override fun check(statement: Statement): List<String> {
        if (statement !is Declaration) return emptyList()

        val varName = statement.name
        if (!validator.isValid(varName)) {
            return listOf("La variable '$varName' debería estar en formato $conventionName.")
        }

        return emptyList()
    }
}
