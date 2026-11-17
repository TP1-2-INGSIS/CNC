package cnc.formatter

import cnc.ast.Expression
import cnc.ast.Statement
import cnc.common.ConfigReader

/** Espaciado a aplicar alrededor de un símbolo (`=`, `:`, operadores). */
enum class Spacing {
    AROUND,
    BEFORE,
    AFTER,
    NONE,
    ;

    fun apply(symbol: String): String =
        when (this) {
            AROUND -> " $symbol "
            BEFORE -> " $symbol"
            AFTER -> "$symbol "
            NONE -> symbol
        }
}

/**
 * Contrato para formatear una sentencia de tipo [T].
 */
interface StatementFormatRule<T : Statement> {
    fun format(
        statement: T,
        formatter: Formatter,
    ): String

    /** Devuelve una instancia de la regla ajustada según las opciones activas en [reader]. */
    fun configure(reader: ConfigReader): StatementFormatRule<T> = this
}

/**
 * Contrato para formatear una expresión de tipo [T].
 */
interface ExpressionFormatRule<T : Expression> {
    fun format(
        expression: T,
        formatter: Formatter,
    ): String

    /** Devuelve una instancia de la regla ajustada según las opciones activas en [reader]. */
    fun configure(reader: ConfigReader): ExpressionFormatRule<T> = this
}
