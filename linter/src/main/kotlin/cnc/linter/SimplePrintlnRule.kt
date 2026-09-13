package cnc.linter

import cnc.ast.BinaryExpression
import cnc.ast.Call
import cnc.ast.Statement

class SimplePrintlnRule : LinterRule {
    override fun check(statement: Statement): List<String> {
        if (statement !is Call || statement.function != "println") return emptyList()

        val warnings = mutableListOf<String>()
        for (arg in statement.arguments) {
            if (arg is BinaryExpression) {
                warnings.add("La llamada a 'println' no permite expresiones complejas.")
            }
        }
        return warnings
    }
}
