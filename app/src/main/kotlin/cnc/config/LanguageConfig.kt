package cnc.config

import cnc.token.Token
import cnc.token.TokenType
import cnc.token.RegexTokenDef
import cnc.token.SymbolTokenDef
import cnc.token.TokenDefinition
import cnc.token.TokenDefinitionProvider

import cnc.ast.ExpressionBuilder
import cnc.ast.NumberLiteral
import cnc.ast.StringLiteral
import cnc.ast.Identifier
import cnc.ast.Declaration
import cnc.ast.Assignment

import cnc.parser.Grammar
import cnc.parser.ExpressionStrat
import cnc.parser.IsStrat
import cnc.parser.AnyStrat
import cnc.parser.AnyOfTypeStrat

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
object PrintScriptTokenDefProvider: TokenDefinitionProvider {
  val definitions = mapOf<TokenType, List<TokenDefinition>> (
    TokenType.OPERATOR      to listOf(
      PlusDefinition,
      MinusDefinition,
      DivisionDefinition,
      MultiplicationDefinition,
      EqualsDefinition
    ),
    TokenType.SYMBOL        to listOf(
      TerminationDefinition,
      TypeDefinition,
      AssignDefinition
    ),
    TokenType.VARIABLE_TYPE to listOf(
      StringTypeDefinition,
      NumberTypeDefinition
    ),
    TokenType.KEYWORD       to listOf(
      VariableDefinition
    ),
    TokenType.IDENTIFIER    to listOf(
      IdentifierDefinition
    ),
    TokenType.NUMBER        to listOf(
      NumberExpressionDefinition
    ),
    TokenType.STRING        to listOf(
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

  override fun getExpressionTokens() : List<TokenDefinition> {
    return listOf(
      NumberExpressionDefinition,
      StringExpressionDefinition,
      IdentifierDefinition,
      PlusDefinition,
      MinusDefinition,
      MultiplicationDefinition,
      DivisionDefinition
    )
  }

}

// GRAMMAR AND STATEMENTS =======================================================
val VariableDeclaration = Grammar(
  tag = "VariableDeclaration",
  sequence = listOf(
    IsStrat(VariableDefinition),       // segments[0] = [let]
    IsStrat(IdentifierDefinition),     // segments[1] = [x]
    IsStrat(TypeDefinition),           // segments[2] = [:]
    AnyOfTypeStrat(PrintScriptTokenDefProvider.getValue(TokenType.VARIABLE_TYPE)!!),    // segments[3] = [number]
    IsStrat(AssignDefinition),         // segments[4] = [=]
    ExpressionStrat(PrintScriptTokenDefProvider.getExpressionTokens()), // segments[5] = [2, *, (, x, +, 3, )]
    IsStrat(TerminationDefinition)     // segments[6] = [;]
  ),
  build = { segments ->
    Declaration(
      name = segments[1].first().text,
      type = segments[3].first().text,
      value = expressionBuilder.build(segments[5])
    )
  }
)

val VariableAssignment = Grammar(
  tag = "VariableAssignment",
  sequence = listOf(
    IsStrat(IdentifierDefinition),     // segments[0] = [x]
    IsStrat(AssignDefinition),         // segments[1] = [=]
    ExpressionStrat(PrintScriptTokenDefProvider.getExpressionTokens()), // segments[2] = [2, *, (, x, +, 3, )]
    IsStrat(TerminationDefinition)     // segments[3] = [;]
  ),
  build = { segments ->
    Assignment(
      target = segments[0].first().text,
      value = expressionBuilder.build(segments[2])
    )
  }
)


// TODO: Crear una clase Provider de las gramaticas  

val terminators: List<TokenDefinition> = listOf(
  TerminationDefinition
)

val grammars = listOf(
  VariableDeclaration,
  VariableAssignment
)


// EXPRESSIONS BUILDER ==========================================================
val expressionBuilder = ExpressionBuilder(mapOf(
  NumberExpressionDefinition to { token: Token -> NumberLiteral(token.text.toDouble()) },
  StringExpressionDefinition to { token: Token -> StringLiteral(token.text.removeSurrounding("\"")) },
  IdentifierDefinition to { token -> Identifier(token.text) }
))
