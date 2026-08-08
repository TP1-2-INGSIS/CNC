package org.config

sealed class BinaryOperatorDef(val def: String, val priority: Number) 
object PlusSign       : BinaryOperatorDef("+", 1)
object MinusSign      : BinaryOperatorDef("-", 1)
object DivisionSign   : BinaryOperatorDef("/", 2)
object MultiplicationSign       : BinaryOperatorDef("*", 2)

sealed class OperatorDef(val def: String)
object Assign     : OperatorDef("=")
object TypeAssign : OperatorDef(":")

// Podemos poner el regex aca. No lo puse porque no se
// que nos diran los profes al respecto. Asique por el momento
// no lo pongo.
sealed interface AtomicDef
object Identifier : AtomicDef
object Numerical  : AtomicDef

sealed class SymbolDef(val def: String)
object Colon : SymbolDef(":")
object SemiColon : SymbolDef(":")
