package cnc.formatter

import cnc.ast.Expression

/**
 * Capacidades de sub-render que el motor entrega a cada regla, para que
 * compongan sin hardcodear espaciado, sin reimplementar el render de expresiones
 * y sin duplicar la lógica de paréntesis (Decisiones 7 y 10).
 */
interface FormatContext {
    /** Renderiza una expresión probando las `ExpressionRule` en orden. */
    fun formatExpression(expression: Expression): String

    /** Renderiza un símbolo/operador aplicando su `FormatRule` de símbolo. */
    fun formatSymbol(symbol: String): String

    /**
     * Renderiza [child] (operando de una expresión binaria) y lo envuelve en
     * paréntesis si su precedencia lo exige respecto de [parentPrecedence],
     * considerando el [side] para la asociatividad (Decisión 10).
     *
     * Centraliza la lógica de parentización: las reglas de expresión binaria la
     * usan en vez de [formatExpression] directo, sin conocer el algoritmo.
     */
    fun formatOperand(child: Expression, parentPrecedence: Int, side: OperandSide): String
}
