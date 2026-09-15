package cnc.interpreter

import cnc.ast.Assignment
import cnc.ast.BinaryExpression
import cnc.ast.Call
import cnc.ast.Declaration
import cnc.ast.Identifier
import cnc.ast.NumberLiteral
import cnc.ast.StringLiteral
import cnc.ast.UnaryExpression
import cnc.common.ErrorType
import cnc.common.Failure
import cnc.common.Success
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class InterpreterTest {

    private val outputBuffer = mutableListOf<String>()
    private lateinit var interpreter: Interpreter
    private lateinit var env: Environment

    @BeforeEach
    fun setUp() {
        outputBuffer.clear()
        interpreter = InterpreterPresets.v1_0 { outputBuffer.add(it) }
        env = Environment()
    }

    @Test
    fun `declare variable and evaluate identifier`() {
        val decl = Declaration(name = "x", type = "number", value = NumberLiteral(42.0), isMutable = true)
        val result = interpreter.interpret(decl, env)

        assertTrue(result is Success)
        assertTrue(env.isDeclared("x"))

        val evalResult = interpreter.evaluate(Identifier("x"), env)
        assertTrue(evalResult is Success)
        assertEquals(42, (evalResult as Success).data)
    }

    @Test
    fun `uninitialized variable read fails with runtime error`() {
        val decl = Declaration(name = "x", type = "number", value = null, isMutable = true)
        val result = interpreter.interpret(decl, env)

        assertTrue(result is Success)
        assertTrue(env.isDeclared("x"))

        val evalResult = interpreter.evaluate(Identifier("x"), env)
        assertTrue(evalResult is Failure)
        val failure = evalResult as Failure
        assertEquals(ErrorType.RUNTIME, failure.type)
        assertTrue(failure.msg.contains("not been initialized"))
    }

    @Test
    fun `uninitialized variable can be assigned and then read`() {
        val decl = Declaration(name = "x", type = "number", value = null, isMutable = true)
        interpreter.interpret(decl, env)

        val assign = Assignment(target = "x", value = NumberLiteral(10.0))
        val assignResult = interpreter.interpret(assign, env)
        assertTrue(assignResult is Success)

        val evalResult = interpreter.evaluate(Identifier("x"), env)
        assertTrue(evalResult is Success)
        assertEquals(10, (evalResult as Success).data)
    }

    @Test
    fun `reassigning immutable variable fails`() {
        val decl = Declaration(name = "c", type = "number", value = NumberLiteral(100.0), isMutable = false)
        interpreter.interpret(decl, env)

        val assign = Assignment(target = "c", value = NumberLiteral(200.0))
        val result = interpreter.interpret(assign, env)

        assertTrue(result is Failure)
        val failure = result as Failure
        assertEquals(ErrorType.RUNTIME, failure.type)
        assertTrue(failure.msg.contains("immutable"))
    }

    @Test
    fun `reassigning mutable variable succeeds`() {
        val decl = Declaration(name = "m", type = "number", value = NumberLiteral(100.0), isMutable = true)
        interpreter.interpret(decl, env)

        val assign = Assignment(target = "m", value = NumberLiteral(200.0))
        val result = interpreter.interpret(assign, env)

        assertTrue(result is Success)
        val evalResult = interpreter.evaluate(Identifier("m"), env)
        assertEquals(200, (evalResult as Success).data)
    }

    @Test
    fun `scoped environment shadowing does not alter parent scope`() {
        val declParent = Declaration("x", "string", StringLiteral("outer"), isMutable = true)
        interpreter.interpret(declParent, env)

        val childEnv = env.createChild()
        val declChild = Declaration("x", "string", StringLiteral("inner"), isMutable = true)
        interpreter.interpret(declChild, childEnv)

        val innerVal = interpreter.evaluate(Identifier("x"), childEnv)
        assertEquals("inner", (innerVal as Success).data)

        val outerVal = interpreter.evaluate(Identifier("x"), env)
        assertEquals("outer", (outerVal as Success).data)
    }

    @Test
    fun `arithmetic binary expressions compute correctly`() {
        // 10 + 5 * 2 = 20
        val expr = BinaryExpression(
            left = NumberLiteral(10.0),
            operator = "+",
            right = BinaryExpression(
                left = NumberLiteral(5.0),
                operator = "*",
                right = NumberLiteral(2.0)
            )
        )
        val result = interpreter.evaluate(expr, env)
        assertTrue(result is Success)
        assertEquals(20, (result as Success).data)
    }

    @Test
    fun `subtraction and division work as expected`() {
        val expr = BinaryExpression(
            left = NumberLiteral(15.0),
            operator = "-",
            right = BinaryExpression(
                left = NumberLiteral(10.0),
                operator = "/",
                right = NumberLiteral(2.0)
            )
        )
        val result = interpreter.evaluate(expr, env)
        assertTrue(result is Success)
        assertEquals(10, (result as Success).data)
    }

    @Test
    fun `division by zero returns runtime failure`() {
        val expr = BinaryExpression(
            left = NumberLiteral(10.0),
            operator = "/",
            right = NumberLiteral(0.0)
        )
        val result = interpreter.evaluate(expr, env)
        assertTrue(result is Failure)
        val failure = result as Failure
        assertEquals(ErrorType.RUNTIME, failure.type)
        assertEquals("Division by zero", failure.msg)
    }

    @Test
    fun `unary negation works on numbers`() {
        val expr = UnaryExpression(
            operator = "-",
            operand = NumberLiteral(42.0)
        )
        val result = interpreter.evaluate(expr, env)
        assertTrue(result is Success)
        assertEquals(-42, (result as Success).data)
    }

    @Test
    fun `unary positive works on numbers`() {
        val expr = UnaryExpression(
            operator = "+",
            operand = NumberLiteral(42.0)
        )
        val result = interpreter.evaluate(expr, env)
        assertTrue(result is Success)
        assertEquals(42, (result as Success).data)
    }

    @Test
    fun `unary operator fails on non-numbers`() {
        val expr = UnaryExpression(
            operator = "-",
            operand = StringLiteral("hello")
        )
        val result = interpreter.evaluate(expr, env)
        assertTrue(result is Failure)
        assertEquals(ErrorType.RUNTIME, (result as Failure).type)
    }

    @Test
    fun `string concatenation with numbers formats whole numbers without decimal point`() {
        val expr = BinaryExpression(
            left = StringLiteral("Result: "),
            operator = "+",
            right = NumberLiteral(42.0)
        )
        val result = interpreter.evaluate(expr, env)
        assertTrue(result is Success)
        assertEquals("Result: 42", (result as Success).data)
    }

    @Test
    fun `string concatenation preserves true decimals`() {
        val expr = BinaryExpression(
            left = StringLiteral("Value: "),
            operator = "+",
            right = NumberLiteral(42.5)
        )
        val result = interpreter.evaluate(expr, env)
        assertTrue(result is Success)
        assertEquals("Value: 42.5", (result as Success).data)
    }

    @Test
    fun `println builtin outputs canonical formatted string`() {
        val call = Call(
            function = "println",
            arguments = listOf(
                StringLiteral("Total:"),
                NumberLiteral(100.0)
            )
        )
        val result = interpreter.interpret(call, env)
        assertTrue(result is Success)
        assertEquals(1, outputBuffer.size)
        assertEquals("Total: 100", outputBuffer[0])
    }

    @Test
    fun `unknown function call returns failure`() {
        val call = Call(
            function = "unknownFunc",
            arguments = listOf(StringLiteral("hi"))
        )
        val result = interpreter.interpret(call, env)
        assertTrue(result is Failure)
        val failure = result as Failure
        assertEquals(ErrorType.RUNTIME, failure.type)
        assertTrue(failure.msg.contains("Unknown function"))
    }

    @Test
    fun `undefined variable evaluation returns failure`() {
        val result = interpreter.evaluate(Identifier("nonexistent"), env)
        assertTrue(result is Failure)
        val failure = result as Failure
        assertEquals(ErrorType.RUNTIME, failure.type)
        assertTrue(failure.msg.contains("Undefined variable"))
    }

    @Test
    fun `interpretAll executes sequentially and aborts on error`() {
        val statements = listOf(
            Declaration("a", "number", NumberLiteral(1.0)),
            Declaration("b", "number", NumberLiteral(2.0)),
            Assignment("nonexistent", NumberLiteral(3.0)), // will fail
            Declaration("c", "number", NumberLiteral(4.0))  // should not execute
        )

        val results = interpreter.interpretAll(statements, env).toList()
        assertEquals(3, results.size)
        assertTrue(results[0] is Success)
        assertTrue(results[1] is Success)
        assertTrue(results[2] is Failure)
        assertFalse(env.isDeclared("c"))
    }

    @Test
    fun `custom builtin method registered via builder executes correctly`() {
        val customBuiltin = BuiltinMethodFactory.builder()
            .name("customUpper")
            .execute { args ->
                val str = args.firstOrNull()?.toString() ?: ""
                Success("ok", str.uppercase())
            }
            .build()

        var capturedVal: String? = null
        val customPrint = BuiltinMethodFactory.builder()
            .name("record")
            .execute { args ->
                capturedVal = args.firstOrNull()?.toString()
                Success("ok", Unit)
            }
            .build()

        val customInterpreter = InterpreterPresets.builder()
            .registerBuiltin(customBuiltin)
            .registerBuiltin(customPrint)
            .build()

        val call = Call("record", listOf(StringLiteral("hello world")))
        val result = customInterpreter.interpret(call, env)
        assertTrue(result is Success)
        assertEquals("hello world", capturedVal)
    }

    @Test
    fun `v1_1 preset executes readInput in CallExpression`() {
        val interp11 = InterpreterPresets.v1_1(
            input = { "Bautista" },
            output = { outputBuffer.add(it) }
        )
        val decl = Declaration(
            name = "name",
            type = "string",
            value = cnc.ast.CallExpression("readInput", listOf(StringLiteral("Tu nombre:"))),
            isMutable = true
        )
        val result = interp11.interpret(decl, env)
        assertTrue(result is Success)

        val evalResult = interp11.evaluate(Identifier("name"), env)
        assertTrue(evalResult is Success)
        assertEquals("Bautista", (evalResult as Success).data)
    }

    @Test
    fun `v1_1 preset executes readEnv in CallExpression`() {
        val interp11 = InterpreterPresets.v1_1(
            envProvider = { if (it == "APP_ENV") "production" else null },
            output = { outputBuffer.add(it) }
        )
        val decl = Declaration(
            name = "envVal",
            type = "string",
            value = cnc.ast.CallExpression("readEnv", listOf(StringLiteral("APP_ENV"))),
            isMutable = true
        )
        val result = interp11.interpret(decl, env)
        assertTrue(result is Success)

        val evalResult = interp11.evaluate(Identifier("envVal"), env)
        assertTrue(evalResult is Success)
        assertEquals("production", (evalResult as Success).data)
    }

    @Test
    fun `v1_1 executes IfStatement branching and block scoping`() {
        val interp11 = InterpreterPresets.v1_1(output = { outputBuffer.add(it) })
        val ifStmt = cnc.ast.IfStatement(
            condition = cnc.ast.BooleanLiteral(true),
            thenBlock = cnc.ast.BlockStatement(listOf(
                Call("println", listOf(StringLiteral("in then block")))
            )),
            elseBlock = cnc.ast.BlockStatement(listOf(
                Call("println", listOf(StringLiteral("in else block")))
            ))
        )

        val result = interp11.interpret(ifStmt, env)
        assertTrue(result is Success)
        assertEquals(listOf("in then block"), outputBuffer)
    }
}
