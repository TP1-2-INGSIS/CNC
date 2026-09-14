package cnc.linter

import cnc.ast.Statement

interface LinterRule {
    fun check(statement: Statement): List<String>
}
