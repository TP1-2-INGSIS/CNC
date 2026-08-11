package org.parser

import org.config.Token
import org.utils.Result
import org.utils.Node

interface Statement  // let a = 1, b = 2
interface Expression // a + b

// if ( condition ) { Statement } else { Statement }

class Declaration // TODO: not implemented yet

object AST;

object Parser {
  fun parse(tokens: Sequence<Token>) : Result<AST> {
    TODO("not implemented yet!")
  }
}
