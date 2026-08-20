package ast

import cnc.ast.Assignment
import cnc.ast.Call
import cnc.ast.Declaration

interface StatementVisitor<R> {
    fun visit(s: Declaration): R;
    fun visit(s: Assignment): R;
    fun visit(s: Call): R;
}