package org.config

data class BinaryOperatorDef(val symbol: String) 
val PlusSign       = BinaryOperatorDef("+")
val MinusSign      = BinaryOperatorDef("-")
val DivisionSign   = BinaryOperatorDef("/")
val MultiplicationSign       = BinaryOperatorDef("*")

data class OperatorDef(val symbol: String)
val Assign     = OperatorDef("=")
val TypeAssign = OperatorDef(":")

// Podemos poner el regex aca. No lo puse porque no se
// que nos diran los profes al respecto. Asique por el 
// momento no lo pongo.
data class AtomicDef (val chars: String) 
val Identifier = AtomicDef("abcdefghijklmnopqrstuvwxyz_")
val Numerical  = AtomicDef("0123456789")

data class SymbolDef(val symbol: String)
val Colon      = SymbolDef(":")
val SemiColon  = SymbolDef(";")
