package cnc.formatter

/**
 * Lado que ocupa un operando dentro de una expresión binaria padre.
 *
 * Relevante para la parentización por asociatividad (Decisión 10): un operando
 * con la misma precedencia que su padre puede necesitar paréntesis según el lado
 * en que aparece y la asociatividad del operador.
 */
enum class OperandSide { LEFT, RIGHT }
