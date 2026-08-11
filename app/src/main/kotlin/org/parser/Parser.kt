package org.parser

import org.config.OperatorDef
import org.config.BinaryOperatorDef
import org.config.AtomicDef
import org.config.Token
import org.utils.Result
import org.utils.Node

sealed interface Statement  // let a = 1, b = 2
sealed interface Expression // a + b

// if ( condition ) { Statement } else { Statement }

// Parser --> Arma ASTs
// AST: arbol con statements y expresiones
// 
//
data class VariableDeclaration() {
  val name: String,
  val type: TokenType
} : Statement

data class Operation(
  val operator: BinaryOperatorDef,
  val childs: Node<Expression>
) : Expression

data class Atomic(
  val value: AtomicDef
) : Expression

class Declaration // TODO: not implemented yet

object AST;

object Parser {
  fun parse(tokens: Sequence<Token>) : Result<AST> {
    TODO("not implemented yet!")
  }
}
