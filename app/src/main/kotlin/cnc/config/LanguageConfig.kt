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
// --> PROVIDER
object PrintScriptTokenDefProvider: TokenDefinitionProvider {
  val definitions = mapOf<TokenType, List<TokenDefinition>> (
    TokenType.OPERATOR      to listOf(
      SymbolTokenDef("plus", "+"),
      SymbolTokenDef("minus", "-"),
      SymbolTokenDef("division", "/"),
      SymbolTokenDef("multiplication", "*"),
      SymbolTokenDef("equals", "==")
    ),
    TokenType.SYMBOL        to listOf(
      SymbolTokenDef("semicolon", ";"),
      SymbolTokenDef("colon", ":"),
      SymbolTokenDef("assign", "=")
    ),
    TokenType.VARIABLE_TYPE to listOf(
      SymbolTokenDef("string_type", "string"),
      SymbolTokenDef("number_type", "number")
    ),
    TokenType.KEYWORD       to listOf(
      SymbolTokenDef("let", "let")
    ),
    TokenType.IDENTIFIER    to listOf(
      RegexTokenDef("identifier", "[a-zA-Z_][a-zA-Z0-9_]*")
    ),
    TokenType.NUMBER        to listOf(
      RegexTokenDef("number_exp", "[0-9]+")
    ),
    TokenType.STRING        to listOf(
      RegexTokenDef("string_exp", "\".*?\"")
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
