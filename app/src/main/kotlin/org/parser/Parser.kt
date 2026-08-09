package org.parser

import org.config.OperatorDef
import org.config.BinaryOperatorDef
import org.config.AtomicDef
import org.config.Token
import org.utils.Result
import org.utils.Node

interface Statement  // let a = 1, b = 2
interface Expression // a + b

// if ( condition ) { Statement } else { Statement }

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
