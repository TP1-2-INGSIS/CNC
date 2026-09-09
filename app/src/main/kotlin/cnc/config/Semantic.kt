package cnc.config

import cnc.semantic.BinaryOpResolver
import cnc.semantic.DefaultSemanticContext
import cnc.semantic.SymbolTable
import cnc.semantic.TypeResolvers

/** Tipos válidos del lenguaje. */
private val validTypes = setOf("number", "string")

/** Reglas de tipos para operadores binarios. */
private val binaryTypeRules: Map<String, BinaryOpResolver> = mapOf(
  "+" to TypeResolvers.additionOrConcat,
  "-" to TypeResolvers.numericOnly("-"),
  "*" to TypeResolvers.numericOnly("*"),
  "/" to TypeResolvers.numericOnly("/"),
  "**" to TypeResolvers.numericOnly("**")
)

/** Contexto semántico de PrintScript, inyectado al SemanticAnalyzer. */
val semanticContext = DefaultSemanticContext(
  symbolTable = SymbolTable(validTypes),
  binaryRules = binaryTypeRules
)
