package cnc.config

import cnc.token.TokenDefinitionProvider
import cnc.token.TokenType
import cnc.token.TokenDefinition

import cnc.definition.*

// unica fuente de verdad
object TokenDef : TokenDefinitionProvider {
  val definitions = mapOf<TokenType, List<TokenDefinition>> (
    TokenType.OPERATOR to listOf(
      PlusDefinition,
      MinusDefinition,
      DivisionDefinition,
      MultiplicationDefinition,
      EqualsDefinition
    ),
    TokenType.SYMBOL to listOf(
      TerminationDefinition,
      TypeDefinition,
      AssignDefinition
    ),
    TokenType.VARIABLE_TYPE to listOf(
      StringTypeDefinition,
      NumberTypeDefinition
    ),
    TokenType.KEYWORD to listOf(VariableDefinition),
    TokenType.IDENTIFIER  to listOf(IdentifierDefinition),
    TokenType.NUMBER      to listOf(NumberExpressionDefinition),
    TokenType.STRING      to listOf(StringExpressionDefinition)
  );

  override fun getValue(type: TokenType) : List<TokenDefinition>? = definitions[type]

  override fun getTypes() : Set<TokenType> = definitions.keys

  override fun type(str: String) : TokenType {
        for (type in getTypes()) {
            val definitions = getValue(type)!!
            
            for (def in definitions) {
                if (!def.match(str)) continue

                return type 
            }
        }

        return TokenType.INVALID
  }
}
