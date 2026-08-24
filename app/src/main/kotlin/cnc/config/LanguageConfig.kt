package cnc.config

import cnc.token.Token
import cnc.token.TokenType
import cnc.token.RegexTokenDef
import cnc.token.SymbolTokenDef
import cnc.token.TokenDefinition
import cnc.token.TokenDefinitionProvider

import cnc.ast.ExpressionBuilder
import cnc.ast.OperatorDef
import cnc.ast.Associativity
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
      SymbolTokenDef("equals", "=="),
      SymbolTokenDef("exponent", "**")
    ),
    TokenType.SYMBOL        to listOf(
      SymbolTokenDef("semicolon", ";"),
      SymbolTokenDef("colon", ":"),
      SymbolTokenDef("assign", "="),
      SymbolTokenDef("open_paren", "("),
      SymbolTokenDef("close_paren", ")")
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
  
  override fun getDefinition(alias: String): TokenDefinition {
    return definitions.values.flatten().first { it.alias == alias }
  } 

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

// GRAMMAR AND STATEMENTS =======================================================

// TODO: Crear una clase Provider de las gramaticas  

val VariableDeclaration = Grammar(
  tag = "VariableDeclaration",
  sequence = listOf(
    IsStrat(PrintScriptTokenDefProvider.getDefinition("let")),       // segments[0] = [let]
    IsStrat(PrintScriptTokenDefProvider.getDefinition("identifier")),     // segments[1] = [x]
    IsStrat(PrintScriptTokenDefProvider.getDefinition("colon")),           // segments[2] = [:]
    AnyOfTypeStrat(PrintScriptTokenDefProvider.getValue(TokenType.VARIABLE_TYPE)!!),    // segments[3] = [number]
    IsStrat(PrintScriptTokenDefProvider.getDefinition("assign")),         // segments[4] = [=]
    ExpressionStrat(listOf(
    PrintScriptTokenDefProvider.getDefinition("number_exp"),
    PrintScriptTokenDefProvider.getDefinition("string_exp"),
    PrintScriptTokenDefProvider.getDefinition("identifier"),
    PrintScriptTokenDefProvider.getDefinition("plus"),
    PrintScriptTokenDefProvider.getDefinition("minus"),
    PrintScriptTokenDefProvider.getDefinition("multiplication"),
    PrintScriptTokenDefProvider.getDefinition("division"),
    PrintScriptTokenDefProvider.getDefinition("open_paren"),
    PrintScriptTokenDefProvider.getDefinition("close_paren")
    )), // segments[5] = [2, *, (, x, +, 3, )]
    IsStrat(PrintScriptTokenDefProvider.getDefinition("semicolon"))     // segments[6] = [;]
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
    IsStrat(PrintScriptTokenDefProvider.getDefinition("identifier")),     // segments[0] = [x]
    IsStrat(PrintScriptTokenDefProvider.getDefinition("assign")),         // segments[1] = [=]
    ExpressionStrat(listOf(
    PrintScriptTokenDefProvider.getDefinition("number_exp"),
    PrintScriptTokenDefProvider.getDefinition("string_exp"),
    PrintScriptTokenDefProvider.getDefinition("identifier"),
    PrintScriptTokenDefProvider.getDefinition("plus"),
    PrintScriptTokenDefProvider.getDefinition("minus"),
    PrintScriptTokenDefProvider.getDefinition("multiplication"),
    PrintScriptTokenDefProvider.getDefinition("division"),
    PrintScriptTokenDefProvider.getDefinition("open_paren"),
    PrintScriptTokenDefProvider.getDefinition("close_paren")
    )), // segments[2] = [2, *, (, x, +, 3, )]
    IsStrat(PrintScriptTokenDefProvider.getDefinition("semicolon"))     // segments[3] = [;]
  ),
  build = { segments ->
    Assignment(
      target = segments[0].first().text,
      value = expressionBuilder.build(segments[2])
    )
  }
)


val terminators: List<TokenDefinition> = listOf(
  PrintScriptTokenDefProvider.getDefinition("semicolon")
)

val grammars = listOf(
  VariableDeclaration,
  VariableAssignment
)

// EXPRESSIONS BUILDER ==========================================================
val expressionBuilder = ExpressionBuilder(
  recipes = mapOf(
    PrintScriptTokenDefProvider.getDefinition("number_exp") to { token: Token -> NumberLiteral(token.text.toDouble()) },
    PrintScriptTokenDefProvider.getDefinition("string_exp") to { token: Token -> StringLiteral(token.text.removeSurrounding("\"")) },
    PrintScriptTokenDefProvider.getDefinition("identifier") to { token -> Identifier(token.text) }
  ),
  operators = listOf(
    OperatorDef(PrintScriptTokenDefProvider.getDefinition("plus"), precedence = 1),
    OperatorDef(PrintScriptTokenDefProvider.getDefinition("minus"), precedence = 1),
    OperatorDef(PrintScriptTokenDefProvider.getDefinition("multiplication"), precedence = 2),
    OperatorDef(PrintScriptTokenDefProvider.getDefinition("division"), precedence = 2),
    OperatorDef(PrintScriptTokenDefProvider.getDefinition("exponent"), precedence = 3, associativity = Associativity.RIGHT)
  ),
  groupOpen = PrintScriptTokenDefProvider.getDefinition("open_paren"),
  groupClose = PrintScriptTokenDefProvider.getDefinition("close_paren")
)
