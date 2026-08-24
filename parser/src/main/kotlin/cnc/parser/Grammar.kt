package cnc.parser

import cnc.ast.Statement
import cnc.token.Token
import cnc.token.TokenDefinition

interface GrammarStrategy {
  // Dado la lista de tokens y un offset, retorna cuántos tokens consumió.
  // Retorna 0 si no matchea.
  fun consume(
    tokens: List<Token>,
    offset: Int,
  ): Int
}

class IsStrat(
  val definition: TokenDefinition,
) : GrammarStrategy {
  override fun consume(
    tokens: List<Token>,
    offset: Int,
  ): Int {
    if (offset >= tokens.size) return 0
    return if (definition.match(tokens[offset].text)) 1 else 0
  }
}

class AnyStrat(
  val strats: List<GrammarStrategy>,
) : GrammarStrategy {
  override fun consume(
    tokens: List<Token>,
    offset: Int,
  ): Int =
    strats.firstNotNullOfOrNull { strat ->
      strat.consume(tokens, offset).takeIf { it > 0 }
    } ?: 0
}

class AnyOfTypeStrat(
  val acceptableTokens: List<TokenDefinition>,
) : GrammarStrategy {
  override fun consume(
    tokens: List<Token>,
    offset: Int,
  ): Int {
    if (offset >= tokens.size) return 0
    val matches = acceptableTokens.any { it.match(tokens[offset].text) }
    return if (matches) 1 else 0
  }
}

class ExpressionStrat(
  val expressionTokens: List<TokenDefinition>, // tokens válidos dentro de una expresión
) : GrammarStrategy {
  override fun consume(
    tokens: List<Token>,
    offset: Int,
  ): Int {
    var count = 0
    var i = offset
    while (i < tokens.size &&
      expressionTokens.any {
        it.match(tokens[i].text)
      }
    ) {
      count++
      i++
    }
    return count // 0 si no consumió nada
  }
}

// ====================
// Strategies Consumer
// ====================
data class Grammar(
  val tag: String,
  val sequence: List<GrammarStrategy>,
  val build: (List<List<Token>>) -> Statement, // ahora recibe segmentos
) {
  fun matches(tokens: List<Token>): Boolean {
    var offset = 0
    for (strat in sequence) {
      val consumed = strat.consume(tokens, offset)
      if (consumed == 0) return false
      offset += consumed
    }
    return offset == tokens.size
  }

  fun segments(tokens: List<Token>): List<List<Token>> {
    val result = mutableListOf<List<Token>>()
    var offset = 0
    for (strat in sequence) {
      val consumed = strat.consume(tokens, offset)
      result.add(tokens.subList(offset, offset + consumed))
      offset += consumed
    }
    return result
  }
}
