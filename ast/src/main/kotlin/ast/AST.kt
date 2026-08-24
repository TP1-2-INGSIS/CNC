package cnc.ast

import ast.StatementVisitor
import ast.ExpressionVisitor
import cnc.token.Token
import cnc.token.TokenDefinition

// TODO: Crear evaluators para cada statement
// el Parser puede crear los AST conociendolos
// y como comparte con el Interpreter no hay problema 
// mientras tengamos los Evaluators de cada statement.

sealed interface Statement {
    fun <R> accept(visitor: StatementVisitor<R>): R;
}
data class Declaration(
    val name: String,
    val type: String,
    val value: Expression?
) : Statement { override fun <R> accept(visitor: StatementVisitor<R>) = visitor.visit(this); }
data class Assignment(
    val target: String,
    val value: Expression
) : Statement {override fun <R> accept(visitor: StatementVisitor<R>) = visitor.visit(this);}
data class Call(
    val function: String,
    val arguments: List<Expression>
) : Statement {override fun <R> accept(visitor: StatementVisitor<R>) = visitor.visit(this);}

sealed interface Expression {
    fun <R> accept(visitor: ExpressionVisitor<R>): R
}
data class NumberLiteral(val value: Double) : Expression {
    override fun <R> accept(visitor: ExpressionVisitor<R>) = visitor.visit(this)
}
data class StringLiteral(val value: String) : Expression {
    override fun <R> accept(visitor: ExpressionVisitor<R>) = visitor.visit(this)
}
data class Identifier(val name: String) : Expression {
    override fun <R> accept(visitor: ExpressionVisitor<R>) = visitor.visit(this)
}
data class BinaryExpression(
    val left: Expression,
    val operator: String,
    val right: Expression
) : Expression {
    override fun <R> accept(visitor: ExpressionVisitor<R>) = visitor.visit(this)
}

class ExpressionBuilder (
  private val recipes: Map<TokenDefinition, (Token) -> Expression>
) {
  fun build(token: Token) : Expression {
    val (_, builder) = recipes.entries.first { (definition, _) -> 
      definition.match(token.text)
    } 
    return builder(token)
  }

  fun build(tokens: List<Token>) : Expression {
    return build(tokens.first())
  }
}

