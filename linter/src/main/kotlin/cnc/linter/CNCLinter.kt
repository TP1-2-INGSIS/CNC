package cnc.linter

import cnc.ast.BlockStatement
import cnc.ast.IfStatement
import cnc.ast.Statement

class CNCLinter(private val rules: List<LinterRule>) {

    fun lint(statements: Sequence<Statement>): List<String> {
        val allWarnings = mutableListOf<String>()

        for (statement in statements) {
            allWarnings.addAll(checkRecursively(statement))
        }

        return allWarnings
    }

    private fun checkRecursively(statement: Statement): List<String> {
        val warnings = mutableListOf<String>()
        
        for (rule in rules) {
            warnings.addAll(rule.check(statement))
        }

        when (statement) {
            is BlockStatement -> {
                for (stmt in statement.statements) {
                    warnings.addAll(checkRecursively(stmt))
                }
            }
            is IfStatement -> {
                warnings.addAll(checkRecursively(statement.thenBlock))
                statement.elseBlock?.let { 
                    warnings.addAll(checkRecursively(it))
                }
            }
            else -> {}
        }
        
        return warnings
    }
}
