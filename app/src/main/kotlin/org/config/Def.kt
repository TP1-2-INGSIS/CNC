package org.config

sealed class BinaryOperatorDef(val def: String, val priority: Number) 
object PlusSign       : BinaryOperatorDef("+", 1)
object MinusSign      : BinaryOperatorDef("-", 1)
object DivisionSign   : BinaryOperatorDef("/", 2)
object MultiplicationSign       : BinaryOperatorDef("*", 2)

sealed class OperatorDef(val def: String)
object Assign     : OperatorDef("=")
object TypeAssign : OperatorDef(":")

sealed interface AtomicDef
object Identifier : AtomicDef
object Numerical  : AtomicDef
