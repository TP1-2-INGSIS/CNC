package cnc.semantic

import cnc.ast.Statement
import cnc.common.Result

class SemanticAnalyzer(
    private val visitor: SemanticVisitor = SemanticVisitor()
) {
    fun analyze(statements: Sequence<Statement>): Sequence<Result<Statement>> = sequence {
        for (statement in statements) {
            yield(statement.accept(visitor))
        }
    }
}
