package cnc.linter

import cnc.ast.GenericStatement

interface LinterRule {
    fun check(statement: GenericStatement): List<String>
}
