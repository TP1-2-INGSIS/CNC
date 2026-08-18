package cnc.definition

import cnc.parser.Grammar
import cnc.parser.IsStrat
import cnc.parser.AnyStrat
import cnc.parser.AnyTypeVariableStrat
import cnc.ast.Declaration
import cnc.config.expressionBuilder
import cnc.config.TokenDef

val VariableDeclaration = Grammar(
  tag = "VariableDeclaration",
  sequence = listOf(
    IsStrat(VariableDefinition),
    IsStrat(IdentifierDefinition),
    IsStrat(TypeDefinition),
    AnyTypeVariableStrat(TokenDef),
    IsStrat(AssignDefinition),
    AnyStrat(listOf(
      IsStrat(NumberExpressionDefinition),
      IsStrat(StringExpressionDefinition)
    )),
    IsStrat(TerminationDefinition)
  ),
  build = { tokens ->
    Declaration(
      name = tokens[1].text,
      type = tokens[3].text,
      value = expressionBuilder.build(tokens[5])
    )
  }
)

val terminator = TerminationDefinition

val grammars = listOf(
  VariableDeclaration
)
