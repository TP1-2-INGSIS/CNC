package cnc.config

import cnc.semantic.BinaryOpResolver
import cnc.semantic.DefaultSemanticContext
import cnc.semantic.SymbolTable
import cnc.semantic.TypeResolvers
import cnc.semantic.UnaryOpResolver

val binaryTypeRules: Map<String, BinaryOpResolver> = mapOf(
  "+" to TypeResolvers.additionOrConcat,
  "-" to TypeResolvers.numericOnly("-"),
  "*" to TypeResolvers.numericOnly("*"),
  "/" to TypeResolvers.numericOnly("/"),
)

val unaryTypeRules: Map<String, UnaryOpResolver> = mapOf(
  "-" to TypeResolvers.unaryNumeric("-")
)

val symbolTable = SymbolTable(validTypes = setOf("number", "string", "boolean"))

val semanticContext = DefaultSemanticContext(
  symbolTable = symbolTable,
  binaryRules = binaryTypeRules,
  unaryRules = unaryTypeRules
)
