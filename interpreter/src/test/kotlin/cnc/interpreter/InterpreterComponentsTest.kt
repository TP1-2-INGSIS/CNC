package cnc.interpreter

import cnc.ast.Assignment
import cnc.ast.BinaryExpression
import cnc.ast.BlockStatement
import cnc.ast.BooleanLiteral
import cnc.ast.Call
import cnc.ast.CallExpression
import cnc.ast.Declaration
import cnc.ast.Expression
import cnc.ast.Identifier
import cnc.ast.IfStatement
import cnc.ast.NumberLiteral
import cnc.ast.Statement
import cnc.ast.StringLiteral
import cnc.ast.UnaryExpression
import cnc.common.Failure
import cnc.common.Success
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

class InterpreterComponentsTest {

    @Nested
    inner class NumberOperationsTests {

        @Test
        fun `operations succeed with valid numbers`() {
            val add = NumberOperations.add(10, 5)
            assertTrue(add is Success)
            assertEquals(15, (add as Success).data)

            val sub = NumberOperations.subtract(10.5, 5.0)
            assertTrue(sub is Success)
            assertEquals(5.5, (sub as Success).data)

            val mul = NumberOperations.multiply(3, 4)
            assertTrue(mul is Success)
            assertEquals(12, (mul as Success).data)

            val div = NumberOperations.divide(20, 4)
            assertTrue(div is Success)
            assertEquals(5, (div as Success).data)

            val neg = NumberOperations.negate(42)
            assertTrue(neg is Success)
            assertEquals(-42, (neg as Success).data)

            val pos = NumberOperations.positive(42)
            assertTrue(pos is Success)
            assertEquals(42, (pos as Success).data)
        }

        @Test
        fun `operations fail on invalid types and division by zero`() {
            assertTrue(NumberOperations.add("abc", 5) is Failure<*>)
            assertTrue(NumberOperations.add(5, "abc") is Failure<*>)
            assertTrue(NumberOperations.subtract("abc", 5) is Failure<*>)
            assertTrue(NumberOperations.subtract(5, "abc") is Failure<*>)
            assertTrue(NumberOperations.multiply("abc", 5) is Failure<*>)
            assertTrue(NumberOperations.multiply(5, "abc") is Failure<*>)
            assertTrue(NumberOperations.divide("abc", 5) is Failure<*>)
            assertTrue(NumberOperations.divide(5, "abc") is Failure<*>)
            assertTrue(NumberOperations.divide(10, 0) is Failure<*>)

            assertTrue(NumberOperations.negate("not a number") is Failure<*>)
            assertTrue(NumberOperations.positive("not a number") is Failure<*>)
        }
    }

    @Nested
    inner class EnvironmentTests {

        @Test
        fun `variable lifecycle in environment scopes`() {
            val env = Environment()

            // Define
            assertTrue(env.define("x", 10, isMutable = true) is Success)
            assertTrue(env.isDeclared("x"))
            assertFalse(env.isDeclared("y"))

            // Redefine in same scope fails
            assertTrue(env.define("x", 20) is Failure<*>)

            // Get
            val xVal = env.get("x")
            assertTrue(xVal is Success)
            assertEquals(10, (xVal as Success).data)

            // Assign mutable
            assertTrue(env.assign("x", 30) is Success)
            assertEquals(30, (env.get("x") as Success).data)

            // Immutable
            assertTrue(env.define("c", 100, isMutable = false) is Success)
            assertTrue(env.assign("c", 200) is Failure<*>)

            // Uninitialized
            assertTrue(env.define("uninit", null, isMutable = true, isInitialized = false) is Success)
            assertTrue(env.get("uninit") is Failure<*>)
            assertTrue(env.assign("uninit", 50) is Success)
            assertEquals(50, (env.get("uninit") as Success).data)

            // Undefined
            assertTrue(env.get("unknown") is Failure<*>)
            assertTrue(env.assign("unknown", 1) is Failure<*>)
        }

        @Test
        fun `child environment shadow and lookup`() {
            val parent = Environment()
            parent.define("global", "hello")
            parent.define("shadowed", 1)

            val child = parent.createChild()
            child.define("shadowed", 2)
            child.define("local", true)

            assertTrue(child.isDeclared("global"))
            assertEquals("hello", (child.get("global") as Success).data)
            assertEquals(2, (child.get("shadowed") as Success).data)
            assertEquals(1, (parent.get("shadowed") as Success).data)

            // Assigning to parent variable from child
            assertTrue(child.assign("global", "world") is Success)
            assertEquals("world", (parent.get("global") as Success).data)
        }
    }

    @Nested
    inner class ExpressionEvaluatorEdgeCases {

        private val interpreter = TestInterpreterFactory.create()
        private val env = Environment()

        @Test
        fun `evaluates call expression and handles missing functions`() {
            val callExpr = CallExpression("readInput", listOf(StringLiteral("prompt: ")))
            val res = interpreter.evaluate(callExpr, env)
            assertTrue(res is Success)

            val missingCall = CallExpression("nonExistent", emptyList())
            val missingRes = interpreter.evaluate(missingCall, env)
            assertTrue(missingRes is Failure<*>)
        }

        @Test
        fun `evaluates boolean literals`() {
            assertEquals(true, (interpreter.evaluate(BooleanLiteral(true), env) as Success).data)
            assertEquals(false, (interpreter.evaluate(BooleanLiteral(false), env) as Success).data)
        }

        @Test
        fun `evaluates binary and unary error branches`() {
            // Unsupported binary operator
            val unsupportedBin = BinaryExpression(NumberLiteral(1.0), "%", NumberLiteral(2.0))
            assertTrue(interpreter.evaluate(unsupportedBin, env) is Failure<*>)

            // Binary operand failure (identifier not declared)
            val failLeft = BinaryExpression(Identifier("unknown"), "+", NumberLiteral(2.0))
            assertTrue(interpreter.evaluate(failLeft, env) is Failure<*>)

            val failRight = BinaryExpression(NumberLiteral(2.0), "+", Identifier("unknown"))
            assertTrue(interpreter.evaluate(failRight, env) is Failure<*>)

            // Unsupported unary operator
            val unsupportedUnary = UnaryExpression("!", BooleanLiteral(true))
            assertTrue(interpreter.evaluate(unsupportedUnary, env) is Failure<*>)

            // Unary operand failure
            val failUnary = UnaryExpression("-", Identifier("unknown"))
            assertTrue(interpreter.evaluate(failUnary, env) is Failure<*>)

            // Unknown expression type
            val dummyExpr = object : Expression {}
            assertTrue(interpreter.evaluate(dummyExpr, env) is Failure<*>)
        }
    }

    @Nested
    inner class StatementEvaluatorsTests {

        private val interpreter = TestInterpreterFactory.create()
        private val env = Environment()

        @Test
        fun `declaration without value remains uninitialized`() {
            val decl = Declaration("y", "number", null)
            assertTrue(interpreter.interpret(decl, env) is Success)
            assertTrue(env.isDeclared("y"))
            assertTrue(env.get("y") is Failure<*>)
        }

        @Test
        fun `assignment failure when value expression fails`() {
            val assign = Assignment("x", Identifier("unresolved"))
            assertTrue(interpreter.interpret(assign, env) is Failure<*>)
        }

        @Test
        fun `call evaluator unknown function and argument failure`() {
            val unknownCall = Call("randomFunc", emptyList())
            assertTrue(interpreter.interpret(unknownCall, env) is Failure<*>)

            val callArgFail = Call("println", listOf(Identifier("unresolved")))
            assertTrue(interpreter.interpret(callArgFail, env) is Failure<*>)
        }

        @Test
        fun `block evaluator error propagation`() {
            val block = BlockStatement(listOf(
                Declaration("a", "number", NumberLiteral(1.0)),
                Assignment("unknown", NumberLiteral(2.0))
            ))
            assertTrue(interpreter.interpret(block, env) is Failure<*>)
        }

        @Test
        fun `if evaluator condition errors and false branch without else`() {
            // Non-boolean condition
            val nonBoolIf = IfStatement(NumberLiteral(42.0), BlockStatement(emptyList()))
            assertTrue(interpreter.interpret(nonBoolIf, env) is Failure<*>)

            // Failing condition
            val failCondIf = IfStatement(Identifier("unresolved"), BlockStatement(emptyList()))
            assertTrue(interpreter.interpret(failCondIf, env) is Failure<*>)

            // Condition false without else block returns Success
            val falseNoElse = IfStatement(BooleanLiteral(false), BlockStatement(emptyList()), null)
            assertTrue(interpreter.interpret(falseNoElse, env) is Success)
        }
    }

    @Nested
    inner class InterpreterBuilderAndOrchestratorTests {

        @Test
        fun `builder single and map registrations and copy`() {
            val builder = Interpreter.builder()
                .registerEvaluator(Declaration::class, DeclarationEvaluator())
                .registerBinaryOperation("+", BinaryOperation(StandardBinaryOperations::add))
                .registerUnaryOperation("-", UnaryOperation(StandardUnaryOperations::negate))
                .registerBuiltin(BuiltinMethodFactory.println())

            val copy = builder.copy()
            val interp = copy.build()
            assertNotNull(interp)

            // Missing evaluator for unsupported statement (e.g. Call is not registered in this builder)
            val callStmt = Call("unregistered", emptyList())
            val res = interp.interpret(callStmt, Environment())
            assertTrue(res is Failure<*>)
        }

        @Test
        fun `interpret list and interpretAll sequence`() {
            val interp = TestInterpreterFactory.create()
            val env = Environment()

            val stmts = listOf(
                Declaration("a", "number", NumberLiteral(10.0)),
                Declaration("b", "number", NumberLiteral(20.0))
            )
            val resList = interp.interpret(stmts, env)
            assertTrue(resList is Success)

            val seqResults = interp.interpretAll(stmts, Environment()).toList()
            assertEquals(2, seqResults.size)
            assertTrue(seqResults.all { it is Success })

            // Sequence terminates on failure
            val failingStmts = listOf(
                Declaration("c", "number", NumberLiteral(1.0)),
                Assignment("unknownVar", NumberLiteral(2.0)),
                Declaration("d", "number", NumberLiteral(3.0))
            )
            val failSeq = interp.interpretAll(failingStmts, Environment()).toList()
            assertEquals(2, failSeq.size)
            assertTrue(failSeq[1] is Failure<*>)
        }
    }

    @Nested
    inner class BuiltinMethodFactoryAndValueFormatterTests {

        @Test
        fun `readEnv with missing or non existent variable`() {
            val envMethod = BuiltinMethodFactory.readEnv { null }

            // Missing argument
            val noArg = envMethod.execute(emptyList())
            assertTrue(noArg is Failure<*>)

            // Not found
            val notFound = envMethod.execute(listOf("MY_VAR"))
            assertTrue(notFound is Failure<*>)

            // Success
            val foundMethod = BuiltinMethodFactory.readEnv { "secret_val" }
            val found = foundMethod.execute(listOf("MY_VAR"))
            assertTrue(found is Success)
            assertEquals("secret_val", (found as Success).data)
        }

        @Test
        fun `value formatter formats all types correctly`() {
            assertEquals("null", ValueFormatter.format(null))
            assertEquals("42", ValueFormatter.format(42.0))
            assertEquals("3.14", ValueFormatter.format(3.14))
            assertEquals("10", ValueFormatter.format(10.0f))
            assertEquals("2.5", ValueFormatter.format(2.5f))
            assertEquals("100", ValueFormatter.format(100.toLong()))
            assertEquals("hello", ValueFormatter.format("hello"))
            assertEquals("true", ValueFormatter.format(true))
            assertEquals("123", ValueFormatter.format(123))

            assertEquals(5, ValueFormatter.formatNumber(5.0))
            assertEquals(5.5, ValueFormatter.formatNumber(5.5))
        }
    }
}
