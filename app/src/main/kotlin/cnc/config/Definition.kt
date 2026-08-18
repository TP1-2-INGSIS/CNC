package cnc.config

import cnc.token.TokenDefinitionProvider
import cnc.token.TokenType
import cnc.token.TokenDefinition

// PRINTSCRIPT LANGUAGE CONFIGURATIONS

// TOKEN DEFINITIONS ================================================================
val PlusDefinition            = SymbolTokenDef("plus", "+")
val MinusDefinition           = SymbolTokenDef("minus", "-")
val DivisionDefinition        = SymbolTokenDef("division", "/")
val MultiplicationDefinition  = SymbolTokenDef("multiplication", "*")
val EqualsDefinition          = SymbolTokenDef("equals", "==")

val TerminationDefinition     = SymbolTokenDef("semicolon", ";")
val TypeDefinition            = SymbolTokenDef("colon", ":")
val AssignDefinition          = SymbolTokenDef("assign", "=")

val VariableDefinition        = SymbolTokenDef("let", "let")

val StringTypeDefinition      = SymbolTokenDef("string_type", "string")
val NumberTypeDefinition      = SymbolTokenDef("number_type", "number")

val IdentifierDefinition       = RegexTokenDef("identifier", "[a-zA-Z_][a-zA-Z0-9_]*")
val NumberExpressionDefinition = RegexTokenDef("number_exp", "[0-9]+")
val StringExpressionDefinition = RegexTokenDef("string_exp", "\".*?\"")

// TOKEN DEFINITIONS PROVIDER
object TokenDefProviders : TokenDefinitionProvider {
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
    TokenType.KEYWORD to listOf(
      VariableDefinition
    ),
    TokenType.IDENTIFIER  to listOf(
      IdentifierDefinition
    ),
    TokenType.NUMBER      to listOf(
      NumberExpressionDefinition
    ),
    TokenType.STRING      to listOf(
      StringExpressionDefinition
    )
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
