package cnc.linter

import cnc.ast.Expression
import cnc.ast.Statement
import cnc.common.ConfigReader
import cnc.common.Failure
import cnc.common.Result
import cnc.common.Success
import com.google.gson.Gson
import com.google.gson.JsonObject
import com.google.gson.JsonPrimitive
import kotlin.reflect.KClass

/** Builder fluido del linter: registra reglas por nodo AST y las auto-configura desde JSON. */
class LinterBuilder(
    initialStatements: Map<KClass<out Statement>, StatementLintRule<*>> = emptyMap(),
    initialExpressions: Map<KClass<out Expression>, ExpressionLintRule<*>> = emptyMap(),
) {
    private val statementRules = initialStatements.toMutableMap()
    private val expressionRules = initialExpressions.toMutableMap()

    fun <T : Statement> registerStatement(
        kClass: KClass<T>,
        rule: StatementLintRule<T>,
    ): LinterBuilder =
        apply {
            statementRules[kClass] = rule
        }

    inline fun <reified T : Statement> registerStatement(rule: StatementLintRule<T>): LinterBuilder =
        registerStatement(T::class, rule)

    fun <T : Expression> registerExpression(
        kClass: KClass<T>,
        rule: ExpressionLintRule<T>,
    ): LinterBuilder =
        apply {
            expressionRules[kClass] = rule
        }

    inline fun <reified T : Expression> registerExpression(rule: ExpressionLintRule<T>): LinterBuilder =
        registerExpression(T::class, rule)

    fun copy(): LinterBuilder = LinterBuilder(statementRules, expressionRules)

    fun build(): CNCLinter = CNCLinter(statementRules.toMap(), expressionRules.toMap())

    fun build(reader: ConfigReader): Result<CNCLinter> {
        val configuredStatements = mutableMapOf<KClass<out Statement>, StatementLintRule<*>>()
        for ((kClass, rule) in statementRules) {
            when (val res = rule.configure(reader)) {
                is Failure -> return Failure(res.msg, res.type)
                is Success -> configuredStatements[kClass] = res.data
            }
        }

        val configuredExpressions = mutableMapOf<KClass<out Expression>, ExpressionLintRule<*>>()
        for ((kClass, rule) in expressionRules) {
            when (val res = rule.configure(reader)) {
                is Failure -> return Failure(res.msg, res.type)
                is Success -> configuredExpressions[kClass] = res.data
            }
        }

        return Success("ok", CNCLinter(configuredStatements, configuredExpressions))
    }

    fun build(jsonString: String?): Result<CNCLinter> {
        val root =
            if (jsonString.isNullOrBlank()) {
                JsonObject()
            } else {
                runCatching {
                    Gson().fromJson(jsonString, JsonObject::class.java)
                }.getOrNull() ?: JsonObject()
            }
        return build(ConfigReader.fromMap(root.toValueMap()))
    }

    fun create(jsonString: String? = null): CNCLinter =
        when (val result = build(jsonString)) {
            is Success -> result.data
            is Failure -> CNCLinter()
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
