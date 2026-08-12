package cnc.parser

import cnc.token.Token

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

  // TODO: esta curioso mas que nada esto
  // no existe otra manera? porque estamos recorriendo devuelta
  // el stream que nos llega desde el principio hasta el final, no?
  for (item in this@splitAfter) {
    current.add(item)
    if (!predicate(item)) continue

    yield(current.toList())
    current.clear()
  }
  
  if (current.isNotEmpty()) yield(current.toList())
}
