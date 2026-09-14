package cnc.linter

import cnc.ast.Statement

class CNCLinter(private val rules: List<LinterRule>) {

    fun lint(statements: Sequence<Statement>): List<String> {
        val allWarnings = mutableListOf<String>()

        for (statement in statements) {
            for (rule in rules) {
                allWarnings.addAll(rule.check(statement))
            }
        }

        return allWarnings
    }
}
