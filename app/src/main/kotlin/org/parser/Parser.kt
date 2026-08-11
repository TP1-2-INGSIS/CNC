package org.parser

import org.config.Token
import org.utils.Result
import org.utils.Node
import org.config.TokenDef  

sealed interface Statement  // let a = 1, b = 2
sealed interface Expression // a + b

// if ( condition ) { Statement } else { Statement }

<<<<<<< HEAD
// Parser --> Arma ASTs
// AST: arbol con statements y expresiones
// 
//
data class VariableDeclaration() {
  val name: String,
  val type: TokenDef
} : Statement

data class Operation(
  val operator: BinaryOperatorDef,
  val childs: Node<Expression>
) : Expression

data class Atomic(
  val value: AtomicDef
) : Expression


=======
class Declaration // TODO: not implemented yet
>>>>>>> main

object AST;

object Parser {
  fun parse(tokens: Sequence<Token>) : Result<AST> {
    TODO("not implemented yet!")
  }
}
