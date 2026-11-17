package cnc.linter

import cnc.ast.Declaration
import cnc.common.ConfigReader
import cnc.common.ErrorType
import cnc.common.Failure
import cnc.common.NamingValidator
import cnc.common.Result
import cnc.common.Success

class CamelCaseValidator : NamingValidator {
    override fun isValid(value: String) = value.matches(Regex("^[a-z][a-z0-9]*([A-Z0-9][a-z0-9]*)*$"))
}

class SnakeCaseValidator : NamingValidator {
    override fun isValid(value: String) = value.matches(Regex("^[a-z][a-z0-9]*(_[a-z0-9]+)*$"))
}

/**
 * Regla de declaraciones: valida la convención de nombres cuando está configurada
 * y delega al [CNCLinter] la expresión inicializadora.
 */
data class DeclarationLintRule(
    val conventionName: String? = null,
    val validator: NamingValidator? = null,
    val aliases: List<String> = DEFAULT_ALIASES,
    val validators: ValidatorsContainer = ValidatorsContainer.default(),
) : StatementLintRule<Declaration> {
    override fun lint(
        statement: Declaration,
        linter: CNCLinter,
    ): List<String> {
        val warnings = mutableListOf<String>()
        val activeValidator = validator
        val activeConvention = conventionName
        if (activeValidator != null && activeConvention != null && !activeValidator.isValid(statement.name)) {
            warnings.add("La variable '${statement.name}' debería estar en formato $activeConvention.")
        }
        statement.value?.let { warnings.addAll(linter.lint(it)) }
        return warnings
    }

    override fun configure(reader: ConfigReader): Result<StatementLintRule<Declaration>> {
        val format =
            reader.readString(aliases)
                ?: return Success("inactive", copy(conventionName = null, validator = null))
        val resolved =
            validators.resolve(format)
                ?: return Failure("Convention validator for '$format' not found", ErrorType.CLI)
        return Success("ok", copy(conventionName = format, validator = resolved))
    }

    fun withValidator(
        key: String,
        validator: NamingValidator,
    ): DeclarationLintRule = copy(validators = validators.withValidator(key, validator))

    companion object {
        val DEFAULT_ALIASES: List<String> =
            listOf("identifier_format", "identifier-format", "naming-convention")
    }
}
