package cnc.formatter

import cnc.ast.Expression
import cnc.ast.Statement
import cnc.common.ConfigReader
import cnc.common.ErrorType
import cnc.common.Failure
import cnc.common.Result
import cnc.common.Success
import com.google.gson.Gson
import com.google.gson.JsonObject
import com.google.gson.JsonPrimitive
import kotlin.reflect.KClass

/**
 * Builder fluido e integrable para ensamblar un [Formatter] con reglas tipadas
 * y auto-configurables para sentencias y expresiones.
 */
class FormatterBuilder {
    private val statementRules = mutableMapOf<KClass<out Statement>, StatementFormatRule<*>>()
    private val expressionRules = mutableMapOf<KClass<out Expression>, ExpressionFormatRule<*>>()
    private var statementSeparator: String = "\n"

    fun <T : Statement> registerStatement(
        kClass: KClass<T>,
        rule: StatementFormatRule<T>,
    ): FormatterBuilder =
        apply {
            statementRules[kClass] = rule
        }

    inline fun <reified T : Statement> registerStatement(rule: StatementFormatRule<T>): FormatterBuilder =
        registerStatement(T::class, rule)

    fun registerStatements(rules: Map<KClass<out Statement>, StatementFormatRule<*>>): FormatterBuilder =
        apply {
            statementRules.putAll(rules)
        }

    fun <T : Expression> registerExpression(
        kClass: KClass<T>,
        rule: ExpressionFormatRule<T>,
    ): FormatterBuilder =
        apply {
            expressionRules[kClass] = rule
        }

    inline fun <reified T : Expression> registerExpression(rule: ExpressionFormatRule<T>): FormatterBuilder =
        registerExpression(T::class, rule)

    fun registerExpressions(rules: Map<KClass<out Expression>, ExpressionFormatRule<*>>): FormatterBuilder =
        apply {
            expressionRules.putAll(rules)
        }

    fun withStatementSeparator(separator: String): FormatterBuilder =
        apply {
            this.statementSeparator = separator
        }

    fun copy(): FormatterBuilder {
        val cloned = FormatterBuilder()
        cloned.statementRules.putAll(this.statementRules)
        cloned.expressionRules.putAll(this.expressionRules)
        cloned.statementSeparator = this.statementSeparator
        return cloned
    }

    fun build(): Formatter =
        Formatter(
            statementRules = statementRules.toMap(),
            expressionRules = expressionRules.toMap(),
            statementSeparator = statementSeparator,
        )

    fun build(reader: ConfigReader): Formatter =
        Formatter(
            statementRules = statementRules.mapValues { (_, rule) -> rule.configure(reader) },
            expressionRules = expressionRules.mapValues { (_, rule) -> rule.configure(reader) },
            statementSeparator = statementSeparator,
        )

    fun build(jsonString: String?): Result<Formatter> {
        if (jsonString.isNullOrBlank()) {
            return Success("ok", build())
        }

        val root =
            runCatching {
                Gson().fromJson(jsonString, JsonObject::class.java)
            }.getOrNull() ?: return Failure("Invalid formatter JSON configuration", ErrorType.CLI)

        val reader = ConfigReader.fromMap(root.toValueMap())
        return Success("ok", build(reader))
    }

    fun create(jsonString: String? = null): Formatter =
        when (val result = build(jsonString)) {
            is Success -> result.data
            is Failure -> build()
        }

    companion object {
        private fun JsonObject.toValueMap(): Map<String, Any?> =
            entrySet().associate { (key, element) ->
                val value: Any? =
                    when {
                        element == null || element.isJsonNull -> null
                        element is JsonPrimitive && element.isBoolean -> element.asBoolean
                        element is JsonPrimitive && element.isNumber -> element.asNumber
                        element is JsonPrimitive -> element.asString
                        else -> element.toString()
                    }
                key to value
            }
    }
}
