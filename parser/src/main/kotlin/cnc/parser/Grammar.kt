package cnc.parser

import cnc.token.Token
import cnc.token.TokenDefinition

import cnc.ast.*

// =============================================================================
// Strategies — definen cómo consumir tokens
// =============================================================================

interface GrammarStrategy {
  fun consume(tokens: List<Token>, offset: Int): Int
}

class IsStrat(val definition: TokenDefinition) : GrammarStrategy {
  override fun consume(tokens: List<Token>, offset: Int): Int {
    if (offset >= tokens.size) return 0
    return if (definition.match(tokens[offset].text)) 1 else 0
  }
}

class AnyStrat(val strats: List<GrammarStrategy>) : GrammarStrategy {
  override fun consume(tokens: List<Token>, offset: Int): Int {
    return strats.firstNotNullOfOrNull { strat ->
      strat.consume(tokens, offset).takeIf { it > 0 }
    } ?: 0
  }
}

class AnyOfTypeStrat(
  val acceptableTokens: List<TokenDefinition>
) : GrammarStrategy {
  override fun consume(tokens: List<Token>, offset: Int): Int {
    if (offset >= tokens.size) return 0
    val matches = acceptableTokens.any { it.match(tokens[offset].text) }
    return if (matches) 1 else 0
  }
}

class ExpressionStrat(
  val expressionTokens: List<TokenDefinition>
) : GrammarStrategy {
  override fun consume(tokens: List<Token>, offset: Int): Int {
    var count = 0
    var i = offset
    while (i < tokens.size && expressionTokens.any { it.match(tokens[i].text) }) {
      count++
      i++
    }
    return count
  }
}

// =============================================================================
// Step — wrapper con label opcional
// =============================================================================

data class Step(
  val strategy: GrammarStrategy,
  val label: String? = null
)

// =============================================================================
// Grammar
// =============================================================================

data class Grammar(
  val tag: String,
  val steps: List<Step>,
  val statementDef: StatementDef? = null,
  val expressionBuilder: ExpressionBuilder? = null
) {

  /**
   * Construye el GenericStatement a partir de los tokens matcheados.
   */
  fun buildStatement(tokens: List<Token>): GenericStatement {
    val def = statementDef ?: error("Grammar '$tag' has no statementDef")
    val exprBuilder = expressionBuilder
    val namedSegments = namedSegments(tokens)

    val data = mutableMapOf<String, Any>()
    for ((fieldName, fieldType) in def.fields) {
      val segment = namedSegments[fieldName]
        ?: error("Grammar '$tag' has no step labeled '$fieldName'")
      data[fieldName] = when (fieldType) {
        FieldType.TEXT -> segment.first().text
        FieldType.EXPRESSION -> {
          requireNotNull(exprBuilder) { "Grammar '$tag' needs an ExpressionBuilder for EXPRESSION fields" }
          exprBuilder.build(segment)
        }
        FieldType.EXPRESSIONS -> {
          requireNotNull(exprBuilder) { "Grammar '$tag' needs an ExpressionBuilder for EXPRESSIONS fields" }
          listOf(exprBuilder.build(segment))
        }
      }
    }

    return GenericStatement(def, Fields(data))
  }

  /**
   * Retorna true si la secuencia de steps matchea los tokens completos.
   */
  fun matches(tokens: List<Token>): Boolean {
    var offset = 0
    for (step in steps) {
      val consumed = step.strategy.consume(tokens, offset)
      if (consumed == 0) return false
      offset += consumed
    }
    return offset == tokens.size
  }

  /**
   * Retorna el progreso del matching (para error reporting).
   */
  fun matchProgress(tokens: List<Token>): MatchProgress {
    var offset = 0
    for ((index, step) in steps.withIndex()) {
      val consumed = step.strategy.consume(tokens, offset)
      if (consumed == 0) return MatchProgress(index, offset)
      offset += consumed
    }
    return if (offset == tokens.size) {
      MatchProgress(steps.size, offset, complete = true)
    } else {
      MatchProgress(steps.size, offset, extraTokens = true)
    }
  }

  /**
   * Segmenta los tokens en un mapa nombre → tokens, solo para steps con label.
   */
  fun namedSegments(tokens: List<Token>): Map<String, List<Token>> {
    val result = mutableMapOf<String, List<Token>>()
    var offset = 0
    for (step in steps) {
      val consumed = step.strategy.consume(tokens, offset)
      if (step.label != null) {
        result[step.label] = tokens.subList(offset, offset + consumed)
      }
      offset += consumed
    }
    return result
  }
}

/**
 * Progreso de matching de una gramática contra una lista de tokens.
 */
data class MatchProgress(
  val strategiesMatched: Int,
  val tokensConsumed: Int,
  val complete: Boolean = false,
  val extraTokens: Boolean = false
)
