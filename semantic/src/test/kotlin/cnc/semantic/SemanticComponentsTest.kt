package cnc.semantic

import cnc.ast.Call
import cnc.ast.Declaration
import cnc.ast.NumberLiteral
import cnc.ast.StringLiteral
import cnc.common.BuiltinMethodBuilder
import cnc.common.Failure
import cnc.common.Success
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

class SemanticComponentsTest {

    @Nested
    inner class SemanticAnalyzerBuilderTests {

        @Test
        fun `builder fluently registers components and creates analyzer`() {
            val customTable = SymbolTable(setOf("customType"))
            val builtin = BuiltinMethodBuilder().name("myPrint").returnType(null).execute { Success("ok", Unit) }.build()

            val builder = SemanticAnalyzer.builder()
                .withSymbolTable(customTable)
                .withValidTypes(setOf("number", "string", "boolean"))
                .registerStatementValidator(Declaration::class, StandardStatementValidators.declaration)
                .registerStatementValidators(mapOf(Call::class to StandardStatementValidators.call))
                .setStatementValidators(mapOf(Declaration::class to StandardStatementValidators.declaration))
                .registerExpressionRule(NumberLiteral::class, StandardExpressionRules.numberLiteral)
                .registerExpressionRules(mapOf(StringLiteral::class to StandardExpressionRules.stringLiteral))
                .setExpressionRules(mapOf(NumberLiteral::class to StandardExpressionRules.numberLiteral))
                .registerBinaryOp("+", TypeResolvers.additionOrConcat)
                .registerBinaryOps(mapOf("-" to TypeResolvers.numericOnly("-")))
                .registerUnaryOp("-", TypeResolvers.unaryNumeric("-"))
                .registerUnaryOps(mapOf("-" to TypeResolvers.unaryNumeric("-")))
                .registerBuiltin("myPrint", builtin)
                .registerBuiltins(mapOf("myPrint" to builtin))

            val analyzer = builder.build()
            assertNotNull(analyzer)

            // Unsupported statement type returns Failure
            val context = analyzer.createContext(customTable)
            val dummyStmt = Call("unsupported", emptyList())
            val res = analyzer.validate(dummyStmt, context)
            assertTrue(res is Failure<*>)
        }
    }

    @Nested
    inner class SymbolTableEdgeCasesTests {

        @Test
        fun `symbol table scope lookups and mutations`() {
            val root = SymbolTable(setOf("number", "string"))
            assertTrue(root.isValidType("number"))
            assertFalse(root.isValidType("boolean"))

            // Invalid type declaration fails
            assertTrue(root.declare("bad", "invalidType") is Failure<*>)

            assertTrue(root.declare("x", "number", isMutable = true) is Success)
            assertFalse(root.isDeclaredInCurrentScope("y"))
            assertTrue(root.isDeclared("x"))
            assertEquals("number", root.typeOf("x"))
            assertEquals(true, root.isMutable("x"))

            // Redeclare in same scope fails
            assertTrue(root.declare("x", "number") is Failure<*>)

            // Immutable
            assertTrue(root.declare("c", "string", isMutable = false) is Success)
            assertEquals(false, root.isMutable("c"))

            // Child scope
            val child = root.createChild()
            assertTrue(child.declare("y", "number") is Success)
            assertTrue(child.isDeclared("x"))
            assertTrue(child.isDeclaredInCurrentScope("y"))
            assertFalse(child.isDeclaredInCurrentScope("x"))

            // Lookup in parent
            assertEquals("number", child.lookup("x")?.type)

            // Unknown variable
            assertNull(child.lookup("unknown"))
            assertNull(child.typeOf("unknown"))
            assertNull(child.isMutable("unknown"))
        }
    }

    @Nested
    inner class TypeResolversAndRuleBuildersTests {

        @Test
        fun `resolver logic and failure branches`() {
            val addRes = TypeResolvers.additionOrConcat.resolve("number", "number")
            assertTrue(addRes is Success)
            assertEquals("number", (addRes as Success).data)

            val concatRes = TypeResolvers.additionOrConcat.resolve("string", "number")
            assertTrue(concatRes is Success)
            assertEquals("string", (concatRes as Success).data)

            val invalidAdd = TypeResolvers.additionOrConcat.resolve("boolean", "number")
            assertTrue(invalidAdd is Failure<*>)

            val invalidSub = TypeResolvers.numericOnly("-").resolve("string", "number")
            assertTrue(invalidSub is Failure<*>)

            val invalidUnary = TypeResolvers.unaryNumeric("-").resolve("string")
            assertTrue(invalidUnary is Failure<*>)
        }

        @Test
        fun `rule builders configuration and validation`() {
            val exprRule = ExpressionRule.builder<NumberLiteral>("numberRule")
                .resolve { _, _ -> Success("ok", "number") }
                .build()

            assertNotNull(exprRule)

            val stmtVal = StatementValidator.builder<Declaration>("declValidator")
                .validate { _, _ -> Success("ok", Unit) }
                .build()

            assertNotNull(stmtVal)
        }
    }
}
