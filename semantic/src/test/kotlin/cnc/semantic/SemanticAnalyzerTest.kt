package cnc.semantic

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
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import kotlin.reflect.KClass

class SemanticAnalyzerTest {
    private lateinit var symbolTable: SymbolTable
    private lateinit var expressionResolver: ExpressionTypeResolver
    private lateinit var analyzer: SemanticAnalyzer

    private val testExpressionRules: Map<KClass<out Expression>, ExpressionRule<out Expression>> =
        mapOf(
            NumberLiteral::class to StandardExpressionRules.numberLiteral,
            StringLiteral::class to StandardExpressionRules.stringLiteral,
            BooleanLiteral::class to StandardExpressionRules.booleanLiteral,
            Identifier::class to StandardExpressionRules.identifier,
            BinaryExpression::class to StandardExpressionRules.binaryExpression,
            UnaryExpression::class to StandardExpressionRules.unaryExpression,
            CallExpression::class to StandardExpressionRules.callExpression,
        )

    private val testStatementValidators: Map<KClass<out Statement>, StatementValidator<*>> =
        mapOf(
            Declaration::class to StandardStatementValidators.declaration,
            Assignment::class to StandardStatementValidators.assignment,
            Call::class to StandardStatementValidators.call,
            BlockStatement::class to StandardStatementValidators.block,
            IfStatement::class to StandardStatementValidators.ifValidator,
        )

    private val binaryRules =
        mapOf(
            "+" to TypeResolvers.additionOrConcat,
            "-" to TypeResolvers.numericOnly("-"),
            "*" to TypeResolvers.numericOnly("*"),
            "/" to TypeResolvers.numericOnly("/"),
        )

    @BeforeEach
    fun setUp() {
        symbolTable = SymbolTable(validTypes = setOf("number", "string"))
        expressionResolver =
            ExpressionTypeResolver(
                rules = testExpressionRules,
                binaryRules = binaryRules,
                unaryRules = mapOf("-" to TypeResolvers.unaryNumeric("-")),
            )
        analyzer =
            SemanticAnalyzer(
                symbolTable = symbolTable,
                expressionResolver = expressionResolver,
                statementValidators = testStatementValidators,
            )
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
        val call =
            Call(
                "println",
                listOf(
                    BinaryExpression(
                        Identifier("x"),
                        "+",
                        NumberLiteral(5.0),
                    ),
                ),
            )
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

    data class CustomBooleanLiteral(
        val value: Boolean,
    ) : Expression

    @Test
    fun `custom expression rule can be composed into semantic context without modifying existing code`() {
        val booleanRule =
            ExpressionRule<CustomBooleanLiteral> { _, _ ->
                Success("ok", "boolean")
            }
        val extendedTable = SymbolTable(validTypes = setOf("number", "string", "boolean"))
        val extendedRules =
            testExpressionRules +
                mapOf(
                    CustomBooleanLiteral::class to booleanRule,
                )
        val customResolver =
            ExpressionTypeResolver(
                rules = extendedRules,
                binaryRules = binaryRules,
            )
        val customAnalyzer =
            SemanticAnalyzer(
                symbolTable = extendedTable,
                expressionResolver = customResolver,
                statementValidators = testStatementValidators,
            )

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
        val ifAnalyzer =
            SemanticAnalyzer(
                symbolTable = table,
                expressionResolver = expressionResolver,
                statementValidators = testStatementValidators,
            )

        val ifStmt =
            IfStatement(
                condition = BooleanLiteral(true),
                thenBlock =
                    BlockStatement(
                        listOf(
                            Declaration("msg", "string", StringLiteral("then"), isMutable = true),
                        ),
                    ),
                elseBlock =
                    BlockStatement(
                        listOf(
                            Declaration("msg", "string", StringLiteral("else"), isMutable = true),
                        ),
                    ),
            )

        val results = ifAnalyzer.analyze(sequenceOf(ifStmt)).toList()
        assertEquals(1, results.size)
        assertTrue(results[0] is Success)
        assertFalse(table.isDeclared("msg"), "Variable declared inside blocks should not leak to root scope")
    }

    @Test
    fun `shadowing of outer variable inside block succeeds`() {
        val table = SymbolTable(validTypes = setOf("number", "string", "boolean"))
        val ifAnalyzer =
            SemanticAnalyzer(
                symbolTable = table,
                expressionResolver = expressionResolver,
                statementValidators = testStatementValidators,
            )

        val outerDecl = Declaration("x", "number", NumberLiteral(1.0))
        val ifStmt =
            IfStatement(
                condition = BooleanLiteral(true),
                thenBlock =
                    BlockStatement(
                        listOf(
                            Declaration("x", "string", StringLiteral("shadowed"), isMutable = true),
                        ),
                    ),
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
        val ifStmt =
            IfStatement(
                condition = NumberLiteral(123.0),
                thenBlock =
                    BlockStatement(
                        listOf(
                            Call("println", listOf(StringLiteral("unreachable"))),
                        ),
                    ),
            )

        val results = analyzer.analyze(sequenceOf(ifStmt)).toList()
        assertEquals(1, results.size)
        assertTrue(results[0] is Failure)
        assertTrue((results[0] as Failure).msg.contains("La condición del 'if' debe ser de tipo 'boolean'"))
    }

    @Test
    fun `variable declared in block cannot be accessed outside block`() {
        val ifStmt =
            IfStatement(
                condition = BooleanLiteral(true),
                thenBlock =
                    BlockStatement(
                        listOf(
                            Declaration("secret", "number", NumberLiteral(42.0)),
                        ),
                    ),
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
        val testBuiltins =
            mapOf(
                "println" to
                    object : cnc.common.BuiltinMethod {
                        override val name = "println"

                        override fun execute(arguments: List<Any?>) = Success("ok", Unit)
                    },
            )
        val strictResolver =
            ExpressionTypeResolver(rules = testExpressionRules, binaryRules = binaryRules, builtins = testBuiltins)
        val strictAnalyzer =
            SemanticAnalyzer(
                symbolTable = symbolTable,
                expressionResolver = strictResolver,
                statementValidators = testStatementValidators,
            )

        val call = Call("unknownFunc", listOf(NumberLiteral(42.0)))
        val results = strictAnalyzer.analyze(sequenceOf(call)).toList()

        assertEquals(1, results.size)
        assertTrue(results[0] is Failure)
        assertTrue((results[0] as Failure).msg.contains("Función desconocida o no declarada: 'unknownFunc'"))
    }

    @Test
    fun `calling void function in CallExpression fails`() {
        val testBuiltins =
            mapOf(
                "println" to
                    object : cnc.common.BuiltinMethod {
                        override val name = "println"
                        override val returnType: String? = null

                        override fun execute(arguments: List<Any?>) = Success("ok", Unit)
                    },
            )
        val strictResolver =
            ExpressionTypeResolver(rules = testExpressionRules, binaryRules = binaryRules, builtins = testBuiltins)
        val strictAnalyzer =
            SemanticAnalyzer(
                symbolTable = symbolTable,
                expressionResolver = strictResolver,
                statementValidators = testStatementValidators,
            )

        val decl = Declaration("x", "string", CallExpression("println", listOf(StringLiteral("hi"))))
        val results = strictAnalyzer.analyze(sequenceOf(decl)).toList()

        assertEquals(1, results.size)
        assertTrue(results[0] is Failure)
        assertTrue((results[0] as Failure).msg.contains("no retorna un valor"))
    }

    @Test
    fun `calling returning function in CallExpression succeeds`() {
        val testBuiltins =
            mapOf(
                "readInput" to
                    object : cnc.common.BuiltinMethod {
                        override val name = "readInput"
                        override val returnType = "string"

                        override fun execute(arguments: List<Any?>) = Success("ok", "test")
                    },
            )
        val strictResolver =
            ExpressionTypeResolver(rules = testExpressionRules, binaryRules = binaryRules, builtins = testBuiltins)
        val strictAnalyzer =
            SemanticAnalyzer(
                symbolTable = symbolTable,
                expressionResolver = strictResolver,
                statementValidators = testStatementValidators,
            )

        val decl = Declaration("name", "string", CallExpression("readInput", listOf(StringLiteral("prompt:"))))
        val results = strictAnalyzer.analyze(sequenceOf(decl)).toList()

        assertEquals(1, results.size)
        assertTrue(results[0] is Success)
    }

    @Test
    fun `logical not operator succeeds on boolean and fails on non-boolean`() {
        val table = SymbolTable(validTypes = setOf("number", "string", "boolean"))
        val unaryWithNot =
            mapOf(
                "-" to TypeResolvers.unaryNumeric("-"),
                "!" to TypeResolvers.booleanNot,
            )
        val notResolver =
            ExpressionTypeResolver(rules = testExpressionRules, binaryRules = binaryRules, unaryRules = unaryWithNot)
        val notAnalyzer =
            SemanticAnalyzer(
                symbolTable = table,
                expressionResolver = notResolver,
                statementValidators = testStatementValidators,
            )

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
        val v10Validators =
            mapOf(
                Declaration::class to StandardStatementValidators.declaration,
                Assignment::class to StandardStatementValidators.assignment,
                Call::class to StandardStatementValidators.call,
            )
        val v10Analyzer =
            SemanticAnalyzer(
                symbolTable = symbolTable,
                expressionResolver = expressionResolver,
                statementValidators = v10Validators,
            )
        val ifStmt =
            IfStatement(
                condition = BooleanLiteral(true),
                thenBlock = BlockStatement(emptyList()),
            )
        val results = v10Analyzer.analyze(sequenceOf(ifStmt)).toList()
        assertEquals(1, results.size)
        assertTrue(results[0] is Failure)
        assertTrue(
            (results[0] as Failure).msg.contains(
                "No hay validador semántico registrado para la sentencia: IfStatement",
            ),
        )
    }

    @Test
    fun `custom statement validator can be composed into SemanticAnalyzer`() {
        var customValidatorCalled = false
        val customCallValidator =
            StatementValidator<Call> { _, _ ->
                customValidatorCalled = true
                Success("ok", Unit)
            }

        val customValidators =
            testStatementValidators +
                mapOf(
                    Call::class to customCallValidator,
                )
        val customAnalyzer =
            SemanticAnalyzer(
                symbolTable = symbolTable,
                expressionResolver = expressionResolver,
                statementValidators = customValidators,
            )

        val call = Call("anyCall", emptyList())
        val results = customAnalyzer.analyze(sequenceOf(call)).toList()

        assertEquals(1, results.size)
        assertTrue(results[0] is Success)
        assertTrue(customValidatorCalled)
    }

    @Test
    fun `SemanticContext encapsulates symbolTable and expressionResolver and branches scopes cleanly`() {
        val rootContext = analyzer.createContext(symbolTable)
        assertEquals(symbolTable, rootContext.symbolTable)
        assertEquals(analyzer.expressionResolver, rootContext.expressionResolver)

        val childContext = rootContext.createChildScope()
        assertEquals(rootContext.expressionResolver, childContext.expressionResolver)
        assertNotEquals(rootContext.symbolTable, childContext.symbolTable)
    }

    @Test
    fun `SemanticContext delegates statement validation and expression resolution directly`() {
        var dispatchedStmt: Statement? = null
        val context =
            SemanticContext(
                symbolTable = symbolTable,
                expressionResolver = expressionResolver,
                statementDispatcher = { stmt, _ ->
                    dispatchedStmt = stmt
                    Success("ok", Unit)
                },
            )

        val call = Call("anyCall", emptyList())
        val result = context.validate(call)
        assertTrue(result is Success)
        assertEquals(call, dispatchedStmt)

        val exprRes = context.resolveExpressionType(NumberLiteral(42.0))
        assertTrue(exprRes is Success)
        assertEquals("number", (exprRes as Success).data)
    }

    @Test
    fun `SemanticContext validateInChildScope executes statements in child scope without polluting parent`() {
        val rootContext = analyzer.createContext(symbolTable)
        val decl = Declaration("scopedVar", "number", NumberLiteral(10.0), isMutable = true)

        val result = rootContext.validateInChildScope(listOf(decl))
        assertTrue(result is Success)
        assertFalse(rootContext.symbolTable.isDeclared("scopedVar"))
    }

    @Test
    fun `StatementValidator builder builds functional validator`() {
        var validatedName = ""
        val validator =
            StatementValidator
                .builder<Declaration>("customDecl")
                .validate { stmt, ctx ->
                    ctx
                        .flow()
                        .expect(stmt.name.isNotBlank()) { "Name must not be blank" }
                        .then { validatedName = stmt.name }
                }.build()

        val ctx = analyzer.createContext()
        val res = validator.validate(Declaration("myVar", "number"), ctx)
        assertTrue(res is Success)
        assertEquals("myVar", validatedName)
    }

    @Test
    fun `ExpressionRule builder builds functional expression rule`() {
        val rule =
            ExpressionRule
                .builder<NumberLiteral>("customNum")
                .resolve { expr, ctx ->
                    ctx
                        .flow()
                        .expect(expr.value >= 0) { "Must be non-negative" }
                        .then { "positive-number" }
                }.build()

        val exprCtx = ExpressionContext(symbolTable::typeOf, expressionResolver)
        val res = rule.resolve(NumberLiteral(5.0), exprCtx)
        assertTrue(res is Success)
        assertEquals("positive-number", (res as Success).data)
    }

    @Test
    fun `SemanticAnalyzer builder constructs and executes complete pipeline`() {
        val customAnalyzer =
            SemanticAnalyzer
                .builder()
                .withValidTypes(setOf("number", "string", "boolean"))
                .registerStatementValidators(testStatementValidators)
                .registerExpressionRules(testExpressionRules)
                .registerBinaryOp("+", TypeResolvers.additionOrConcat)
                .build()

        val decl = Declaration("total", "number", BinaryExpression(NumberLiteral(1.0), "+", NumberLiteral(2.0)))
        val results = customAnalyzer.analyze(sequenceOf(decl)).toList()
        assertEquals(1, results.size)
        assertTrue(results[0] is Success)
    }
}
