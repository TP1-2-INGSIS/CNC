package cnc.parser

import cnc.token.Token
import cnc.token.TokenDefinition
import cnc.ast.Statement

/**
 * Error de parsing con información de posición y contexto.
 */
class ParseException(
  message: String,
  val token: Token? = null
) : RuntimeException(message)

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
    if (grammar != null) {
      return grammar.build(grammar.segments(tokens))
    }

    // Buscar la gramática que más avanzó para dar mejor error
    val bestAttempt = grammars
      .map { g -> g to g.matchProgress(tokens) }
      .maxByOrNull { (_, progress) -> progress.strategiesMatched }

    val errorToken = if (bestAttempt != null) {
      val (_, progress) = bestAttempt
      tokens.getOrNull(progress.tokensConsumed)
    } else null

    val position = errorToken?.pos
    val posStr = if (position != null) " at row ${position.row}, col ${position.col}" else ""
    val foundStr = if (errorToken != null) ", found '${errorToken.text}'" else ""

    val bestGrammar = bestAttempt?.first
    val progress = bestAttempt?.second

    val message = buildString {
      append("Syntax error$posStr")
      if (bestGrammar != null && progress != null && progress.strategiesMatched > 0) {
        append(": parsing ${bestGrammar.tag}")
        append(", matched ${progress.strategiesMatched}/${bestGrammar.sequence.size} parts")
        append(foundStr)
      } else {
        append(": no grammar matches [${tokens.joinToString(" ") { it.text }}]")
      }
    }

    throw ParseException(message, errorToken)
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
