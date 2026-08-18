package cnc.definition

import cnc.parser.Grammar
import cnc.parser.ExpressionStrat
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
    IsStrat(VariableDefinition),       // segments[0] = [let]
    IsStrat(IdentifierDefinition),     // segments[1] = [x]
    IsStrat(TypeDefinition),           // segments[2] = [:]
    AnyTypeVariableStrat(TokenDef),    // segments[3] = [number]
    IsStrat(AssignDefinition),         // segments[4] = [=]
    ExpressionStrat(expressionTokens), // segments[5] = [2, *, (, x, +, 3, )]
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
    ExpressionStrat(expressionTokens), // segments[2] = [2, *, (, x, +, 3, )]
    IsStrat(TerminationDefinition)     // segments[3] = [;]
  ),
  build = { segments ->
    Assignment(
      target = segments[0].first().text,
      value = expressionBuilder.build(segments[2])
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
