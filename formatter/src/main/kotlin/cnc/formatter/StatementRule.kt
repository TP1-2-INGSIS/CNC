package cnc.formatter

import cnc.ast.Statement

/**
 * Regla de formato de statements con self-dispatch (Decisión 9), análoga a
 * [ExpressionRule]: intenta formatear el statement y devuelve `null` si no le
 * aplica. Reemplaza el antiguo dispatch por `tag` tras el refactor del AST
 * tipado (`Declaration`/`Assignment`/`Call`) de main.
 */
fun interface StatementRule {
    fun tryFormat(statement: Statement, context: FormatContext): String?
}
