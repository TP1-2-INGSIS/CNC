package cnc.semantic

import cnc.ast.*
import cnc.common.Failure
import cnc.common.Success
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class SemanticAnalyzerTest {

    private lateinit var symbolTable: SymbolTable
    private lateinit var context: DefaultSemanticContext
    private lateinit var analyzer: SemanticAnalyzer

    private val binaryRules = mapOf(
        "+" to TypeResolvers.additionOrConcat,
        "-" to TypeResolvers.numericOnly("-"),
        "*" to TypeResolvers.numericOnly("*"),
        "/" to TypeResolvers.numericOnly("/")
    )

    @BeforeEach
    fun setUp() {
        symbolTable = SymbolTable(validTypes = setOf("number", "string"))
        context = DefaultSemanticContext(symbolTable, binaryRules)
        analyzer = SemanticAnalyzer(context)
    }

    @Test
    fun `valid declaration succeeds and records variable`() {
        val stmt = Declaration("x", "number", NumberLiteral(10.0))
        val results = analyzer.analyze(sequenceOf(stmt)).toList()

        assertEquals(1, results.size)
        assertTrue(results[0] is Success)
        assertTrue(symbolTable.isDeclared("x"))
        assertEquals("number", symbolTable.typeOf("x"))
    }

    @Test
    fun `redeclaration of variable fails`() {
        val first = Declaration("x", "number", NumberLiteral(10.0))
        val second = Declaration("x", "number", NumberLiteral(20.0))
        val results = analyzer.analyze(sequenceOf(first, second)).toList()

        assertEquals(2, results.size)
        assertTrue(results[0] is Success)
        assertTrue(results[1] is Failure)
        assertTrue((results[1] as Failure).msg.contains("ya fue declarada"))
    }

    @Test
    fun `declaration with invalid type fails`() {
        val stmt = Declaration("x", "boolean", NumberLiteral(10.0))
        val results = analyzer.analyze(sequenceOf(stmt)).toList()

        assertEquals(1, results.size)
        assertTrue(results[0] is Failure)
        assertTrue((results[0] as Failure).msg.contains("no reconocido"))
    }

    @Test
    fun `declaration with mismatched expression type fails`() {
        val stmt = Declaration("x", "number", StringLiteral("hello"))
        val results = analyzer.analyze(sequenceOf(stmt)).toList()

        assertEquals(1, results.size)
        assertTrue(results[0] is Failure)
        assertTrue((results[0] as Failure).msg.contains("Se esperaba 'number' pero se obtuvo 'string'"))
    }

    @Test
    fun `valid assignment to declared variable succeeds`() {
        val decl = Declaration("x", "number", NumberLiteral(10.0))
        val assign = Assignment("x", NumberLiteral(20.0))
        val results = analyzer.analyze(sequenceOf(decl, assign)).toList()

        assertEquals(2, results.size)
        assertTrue(results[0] is Success)
        assertTrue(results[1] is Success)
    }

    @Test
    fun `assignment to undeclared variable fails`() {
        val assign = Assignment("y", NumberLiteral(20.0))
        val results = analyzer.analyze(sequenceOf(assign)).toList()

        assertEquals(1, results.size)
        assertTrue(results[0] is Failure)
        assertTrue((results[0] as Failure).msg.contains("no declarada"))
    }

    @Test
    fun `assignment with mismatched type fails`() {
        val decl = Declaration("x", "number", NumberLiteral(10.0))
        val assign = Assignment("x", StringLiteral("hello"))
        val results = analyzer.analyze(sequenceOf(decl, assign)).toList()

        assertEquals(2, results.size)
        assertTrue(results[0] is Success)
        assertTrue(results[1] is Failure)
        assertTrue((results[1] as Failure).msg.contains("No se puede asignar 'string' a 'x' de tipo 'number'"))
    }

    @Test
    fun `call with valid expressions succeeds`() {
        val decl = Declaration("x", "number", NumberLiteral(10.0))
        val call = Call("println", listOf(
            BinaryExpression(
                Identifier("x"),
                "+",
                NumberLiteral(5.0)
            )
        ))
        val results = analyzer.analyze(sequenceOf(decl, call)).toList()

        assertEquals(2, results.size)
        assertTrue(results[0] is Success)
        assertTrue(results[1] is Success)
    }

    @Test
    fun `call with undeclared variable fails`() {
        val call = Call("println", listOf(Identifier("unknown")))
        val results = analyzer.analyze(sequenceOf(call)).toList()

        assertEquals(1, results.size)
        assertTrue(results[0] is Failure)
        assertTrue((results[0] as Failure).msg.contains("no declarada"))
    }

    data class CustomBooleanLiteral(val value: Boolean) : Expression

    @Test
    fun `custom expression rule can be composed into semantic context without modifying existing code`() {
        val booleanRule = ExpressionTypeRule<CustomBooleanLiteral> { _, _ ->
            Success("ok", "boolean")
        }
        val extendedTable = SymbolTable(validTypes = setOf("number", "string", "boolean"))
        val extendedRules = StandardExpressionTypeRules.v1_1 + mapOf(
            CustomBooleanLiteral::class to booleanRule
        )
        val customContext = DefaultSemanticContext(extendedTable, binaryRules, expressionRules = extendedRules)
        val customAnalyzer = SemanticAnalyzer(customContext)

        val decl = Declaration("flag", "boolean", CustomBooleanLiteral(true))
        val results = customAnalyzer.analyze(sequenceOf(decl)).toList()

        assertEquals(1, results.size)
        assertTrue(results[0] is Success)
        assertEquals("boolean", extendedTable.typeOf("flag"))
    }

    @Test
    fun `unary numeric negation succeeds on number`() {
        val decl = Declaration("neg", "number", UnaryExpression("-", NumberLiteral(5.0)))
        val results = analyzer.analyze(sequenceOf(decl)).toList()

        assertEquals(1, results.size)
        assertTrue(results[0] is Success)
    }

    @Test
    fun `unary numeric negation fails on string`() {
        val decl = Declaration("neg", "number", UnaryExpression("-", StringLiteral("hello")))
        val results = analyzer.analyze(sequenceOf(decl)).toList()

        assertEquals(1, results.size)
        assertTrue(results[0] is Failure)
        assertTrue((results[1 - 1] as Failure).msg.contains("Operador unario '-'"))
    }

    @Test
    fun `same variable name declared in then and else blocks succeeds`() {
        val table = SymbolTable(validTypes = setOf("number", "string", "boolean"))
        val ctx = DefaultSemanticContext(table, binaryRules)
        val ifAnalyzer = SemanticAnalyzer(ctx)

        val ifStmt = IfStatement(
            condition = BooleanLiteral(true),
            thenBlock = BlockStatement(listOf(
                Declaration("msg", "string", StringLiteral("then"), isMutable = true)
            )),
            elseBlock = BlockStatement(listOf(
                Declaration("msg", "string", StringLiteral("else"), isMutable = true)
            ))
        )

        val results = ifAnalyzer.analyze(sequenceOf(ifStmt)).toList()
        assertEquals(1, results.size)
        assertTrue(results[0] is Success)
        assertFalse(table.isDeclared("msg"), "Variable declared inside blocks should not leak to root scope")
    }

    @Test
    fun `shadowing of outer variable inside block succeeds`() {
        val table = SymbolTable(validTypes = setOf("number", "string", "boolean"))
        val ctx = DefaultSemanticContext(table, binaryRules)
        val ifAnalyzer = SemanticAnalyzer(ctx)

        val outerDecl = Declaration("x", "number", NumberLiteral(1.0))
        val ifStmt = IfStatement(
            condition = BooleanLiteral(true),
            thenBlock = BlockStatement(listOf(
                Declaration("x", "string", StringLiteral("shadowed"), isMutable = true)
            ))
        )

        val results = ifAnalyzer.analyze(sequenceOf(outerDecl, ifStmt)).toList()
        assertEquals(2, results.size)
        assertTrue(results[0] is Success)
        assertTrue(results[1] is Success)
        assertEquals("number", table.typeOf("x"), "Outer variable type should remain intact")
    }

    @Test
    fun `reassigning a const variable fails in semantic analysis`() {
        val constDecl = Declaration("PI", "number", NumberLiteral(3.14), isMutable = false)
        val assign = Assignment("PI", NumberLiteral(3.14159))

        val results = analyzer.analyze(sequenceOf(constDecl, assign)).toList()
        assertEquals(2, results.size)
        assertTrue(results[0] is Success)
        assertTrue(results[1] is Failure)
        assertTrue((results[1] as Failure).msg.contains("No se puede reasignar la constante 'PI'"))
    }

    @Test
    fun `if with non-boolean condition fails in semantic analysis`() {
        val ifStmt = IfStatement(
            condition = NumberLiteral(123.0),
            thenBlock = BlockStatement(listOf(
                Call("println", listOf(StringLiteral("unreachable")))
            ))
        )

        val results = analyzer.analyze(sequenceOf(ifStmt)).toList()
        assertEquals(1, results.size)
        assertTrue(results[0] is Failure)
        assertTrue((results[0] as Failure).msg.contains("La condición del 'if' debe ser de tipo 'boolean'"))
    }

    @Test
    fun `variable declared in block cannot be accessed outside block`() {
        val ifStmt = IfStatement(
            condition = BooleanLiteral(true),
            thenBlock = BlockStatement(listOf(
                Declaration("secret", "number", NumberLiteral(42.0))
            ))
        )
        val call = Call("println", listOf(Identifier("secret")))

        val results = analyzer.analyze(sequenceOf(ifStmt, call)).toList()
        assertEquals(2, results.size)
        assertTrue(results[0] is Success)
        assertTrue(results[1] is Failure)
        assertTrue((results[1] as Failure).msg.contains("Variable 'secret' no declarada"))
    }

    @Test
    fun `unknown function in Call fails when builtins are registered`() {
        val testBuiltins = mapOf(
            "println" to object : cnc.common.BuiltinMethod {
                override val name = "println"
                override fun execute(arguments: List<Any?>) = Success("ok", Unit)
            }
        )
        val strictContext = DefaultSemanticContext(symbolTable, binaryRules, builtins = testBuiltins)
        val strictAnalyzer = SemanticAnalyzer(strictContext)

        val call = Call("unknownFunc", listOf(NumberLiteral(42.0)))
        val results = strictAnalyzer.analyze(sequenceOf(call)).toList()

        assertEquals(1, results.size)
        assertTrue(results[0] is Failure)
        assertTrue((results[0] as Failure).msg.contains("Función desconocida o no declarada: 'unknownFunc'"))
    }

    @Test
    fun `calling void function in CallExpression fails`() {
        val testBuiltins = mapOf(
            "println" to object : cnc.common.BuiltinMethod {
                override val name = "println"
                override val returnType: String? = null
                override fun execute(arguments: List<Any?>) = Success("ok", Unit)
            }
        )
        val strictContext = DefaultSemanticContext(symbolTable, binaryRules, builtins = testBuiltins)
        val strictAnalyzer = SemanticAnalyzer(strictContext)

        val decl = Declaration("x", "string", CallExpression("println", listOf(StringLiteral("hi"))))
        val results = strictAnalyzer.analyze(sequenceOf(decl)).toList()

        assertEquals(1, results.size)
        assertTrue(results[0] is Failure)
        assertTrue((results[0] as Failure).msg.contains("no retorna un valor"))
    }

    @Test
    fun `calling returning function in CallExpression succeeds`() {
        val testBuiltins = mapOf(
            "readInput" to object : cnc.common.BuiltinMethod {
                override val name = "readInput"
                override val returnType = "string"
                override fun execute(arguments: List<Any?>) = Success("ok", "test")
            }
        )
        val strictContext = DefaultSemanticContext(symbolTable, binaryRules, builtins = testBuiltins)
        val strictAnalyzer = SemanticAnalyzer(strictContext)

        val decl = Declaration("name", "string", CallExpression("readInput", listOf(StringLiteral("prompt:"))))
        val results = strictAnalyzer.analyze(sequenceOf(decl)).toList()

        assertEquals(1, results.size)
        assertTrue(results[0] is Success)
    }

    @Test
    fun `logical not operator succeeds on boolean and fails on non-boolean`() {
        val table = SymbolTable(validTypes = setOf("number", "string", "boolean"))
        val unaryWithNot = mapOf(
            "-" to TypeResolvers.unaryNumeric("-"),
            "!" to TypeResolvers.booleanNot
        )
        val ctx = DefaultSemanticContext(table, binaryRules, unaryRules = unaryWithNot)
        val notAnalyzer = SemanticAnalyzer(ctx)

        val validNot = Declaration("flag", "boolean", UnaryExpression("!", BooleanLiteral(false)))
        val resultsValid = notAnalyzer.analyze(sequenceOf(validNot)).toList()
        assertEquals(1, resultsValid.size)
        assertTrue(resultsValid[0] is Success)

        val invalidNot = Declaration("badFlag", "boolean", UnaryExpression("!", NumberLiteral(10.0)))
        val resultsInvalid = notAnalyzer.analyze(sequenceOf(invalidNot)).toList()
        assertEquals(1, resultsInvalid.size)
        assertTrue(resultsInvalid[0] is Failure)
        assertTrue((resultsInvalid[0] as Failure).msg.contains("Operador unario '!' requiere 'boolean'"))
    }

    @Test
    fun `PrintScript 1_0 analyzer rejects IfStatement because validator is not registered`() {
        val v10Analyzer = SemanticAnalyzer(context, statementValidators = StandardStatementValidators.v1_0)
        val ifStmt = IfStatement(
            condition = BooleanLiteral(true),
            thenBlock = BlockStatement(emptyList())
        )
        val results = v10Analyzer.analyze(sequenceOf(ifStmt)).toList()
        assertEquals(1, results.size)
        assertTrue(results[0] is Failure)
        assertTrue((results[0] as Failure).msg.contains("No hay validador semántico registrado para la sentencia: IfStatement"))
    }

    @Test
    fun `custom statement validator can be composed into SemanticAnalyzer`() {
        var customValidatorCalled = false
        val customCallValidator = StatementValidator<Call> { _, _, _ ->
            customValidatorCalled = true
            Success("ok", Unit)
        }

        val customValidators = StandardStatementValidators.v1_1 + mapOf(
            Call::class to customCallValidator
        )
        val customAnalyzer = SemanticAnalyzer(context, statementValidators = customValidators)

        val call = Call("anyCall", emptyList())
        val results = customAnalyzer.analyze(sequenceOf(call)).toList()

        assertEquals(1, results.size)
        assertTrue(results[0] is Success)
        assertTrue(customValidatorCalled)
    }
}
