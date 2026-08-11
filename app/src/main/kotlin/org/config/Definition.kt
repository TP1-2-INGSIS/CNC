package org.config

data class Definition(val tag: String, val symbol: String) 
data class MultipleDef(val defs: Set<Definition>)

val BinaryOperatorDef = MultipleDef(setOf(
  Definition("PLUS"       ,"+"),
  Definition("MINUS"      ,"-"), 
  Definition("DIVISION"   ,"/"),
  Definition("Multiplier" ,"*")
))

val SymbolDef = MultipleDef(setOf(
  Definition("ASSIGN"   , "="),
  Definition("COLON"    , ":"),
  Definition("SEMICOLON", ";")
))

val AtomicDef = MultipleDef(setOf(
  Definition("IDENTIFIER" , "abcdefghijklmnopqrstuvwxyz_"),
  Definition("LITERAL"    , "0123456789")
))

val KeyWordsDef = MultipleDef(setOf(
  Definition("LET"    , "let"),
  Definition("NUMBER" , "Number"),
  Definition("STRING" , "String")
))
