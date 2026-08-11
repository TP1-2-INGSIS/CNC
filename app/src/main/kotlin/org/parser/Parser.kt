package org.parser

import org.config.Token
import org.config.TerminationDefinition

sealed interface Statement
data class Declaration(
    val name: String,
    val type: String,
    val value: Expression?
) : Statement
data class Assignment(
    val target: String,
    val value: Expression
) : Statement
data class Call(
    val function: String,
    val arguments: List<Expression>
) : Statement

sealed interface Expression
data class NumberLiteral(val value: Double) : Expression
data class StringLiteral(val value: String) : Expression
data class Identifier(val name: String) : Expression
data class BinaryExpression(
    val left: Expression,
    val operator: String,
    val right: Expression
) : Expression

//
// Agrupar los tokens entre los termination tokens
// Armar el AST con esos tokens
//
object Parser {
  fun getAST(tokens: Sequence<Token>): Sequence<Statement> {
    return tokens
      .splitAfter { TerminationDefinition.match(it.text) }
      .map { parseStatement(it) }
  }

  fun parseStatement(tokens: List<Token>): Statement {
    val grammar = grammars.first { it.matches(tokens) }
    return grammar.build(tokens)
  }
}

// Retorna un stream de listas con nuestros elementos separados por el predicado.
// -> predicate: es la lambda que se evalua para comprobar si el elemento es un punto de corte en el stream
fun <T> Sequence<T>.splitAfter(predicate: (T) -> Boolean): Sequence<List<T>> = sequence {
  val current = mutableListOf<T>()

  for (item in this@splitAfter) {
    current.add(item)
    if (!predicate(item)) continue

    yield(current.toList())
    current.clear()
  }
  
  if (current.isNotEmpty()) yield(current.toList())
}
