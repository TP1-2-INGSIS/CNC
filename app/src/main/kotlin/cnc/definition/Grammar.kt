package cnc.definition

import cnc.parser.Grammar
import cnc.parser.IsStrat
import cnc.parser.AnyStrat
import cnc.parser.AnyTypeVariableStrat
import cnc.ast.Declaration
import cnc.ast.Assignment
import cnc.config.expressionBuilder
import cnc.config.TokenDef
import cnc.token.TokenDefinition

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

val VariableAssignment = Grammar(
  tag = "VariableAssignment",
  sequence = listOf(
    IsStrat(IdentifierDefinition),
    IsStrat(AssignDefinition),
    AnyStrat(listOf(
      IsStrat(NumberExpressionDefinition),
      IsStrat(StringExpressionDefinition)
    )),
    IsStrat(TerminationDefinition)
  ),
  build = { tokens ->
    Assignment(
      target = tokens[0].text,
      value = expressionBuilder.build(tokens[2])
    )
    
  }
)

val terminators: List<TokenDefinition> = listOf(
  TerminationDefinition
)

val grammars = listOf(
  VariableDeclaration,
  VariableAssignment
)
