package cnc.linter

import cnc.ast.GenericStatement

class CNCLinter(private val rules: List<LinterRule>) {

    fun lint(statements: Sequence<GenericStatement>): List<String> {
        val allWarnings = mutableListOf<String>()

        for (statement in statements) {
            for (rule in rules) {
                allWarnings.addAll(rule.check(statement))
            }
        }

        return allWarnings
    }
}
