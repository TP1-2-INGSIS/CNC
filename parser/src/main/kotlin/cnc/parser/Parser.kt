package cnc.parser

import cnc.token.Token
import cnc.token.TokenDefinition
import cnc.ast.Statement
//
// Agrupar los tokens entre los termination tokens
// Armar el AST con esos tokens
//
class Parser(
  private val grammars: List<Grammar>,
  private val terminators: List<TokenDefinition>
) {
  fun getASTs(tokens: Sequence<Token>): Sequence<Statement> {
    return tokens
      .splitAfter { terminators.any { t -> t.match(it.text) } }
      .map { parseStatement(it) }
  }

  private fun parseStatement(tokens: List<Token>): Statement {
    val grammar = grammars.firstOrNull { it.matches(tokens) }
      ?: error("No grammar matches tokens: ${tokens.map { it.text }}")
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
