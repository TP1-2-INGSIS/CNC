package cnc.parser

import cnc.token.Token
import cnc.token.TokenDefinition
import cnc.ast.GenericStatement

/**
 * Error de parsing con información de posición y contexto.
 */
class ParseException(
  message: String,
  val token: Token? = null
) : RuntimeException(message)

/**
 * Agrupa los tokens entre los termination tokens y
 * matchea cada grupo contra las gramáticas para construir GenericStatements.
 */
class Parser(
  private val grammars: List<Grammar>,
  private val terminators: List<TokenDefinition>
) {
  fun getASTs(tokens: Sequence<Token>): Sequence<GenericStatement> {
    return tokens
      .splitAfter { terminators.any { t -> t.match(it.text) } }
      .map { parseStatement(it) }
  }

  private fun parseStatement(tokens: List<Token>): GenericStatement {
    val grammar = grammars.firstOrNull { it.matches(tokens) }
    if (grammar != null) {
      return grammar.buildStatement(tokens)
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
        append(", matched ${progress.strategiesMatched}/${bestGrammar.steps.size} parts")
        append(foundStr)
      } else {
        append(": no grammar matches [${tokens.joinToString(" ") { it.text }}]")
      }
    }

    throw ParseException(message, errorToken)
  }
}

// Retorna un stream de listas con nuestros elementos separados por el predicado.
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
