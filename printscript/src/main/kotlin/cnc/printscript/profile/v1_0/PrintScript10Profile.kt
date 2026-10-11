package cnc.printscript.profile.v1_0

import cnc.ast.Assignment
import cnc.ast.Call
import cnc.ast.Declaration
import cnc.ast.Identifier
import cnc.ast.NumberLiteral
import cnc.ast.StringLiteral
import cnc.formatter.Formatter
import cnc.formatter.FormatterFactory
import cnc.interpreter.AssignmentEvaluator
import cnc.interpreter.BinaryOperation
import cnc.interpreter.BuiltinMethodFactory
import cnc.interpreter.CallEvaluator
import cnc.interpreter.DeclarationEvaluator
import cnc.interpreter.Interpreter
import cnc.interpreter.InterpreterBuilder
import cnc.interpreter.StandardBinaryOperations
import cnc.interpreter.StandardUnaryOperations
import cnc.interpreter.UnaryOperation
import cnc.lexer.Lexer
import cnc.lexer.rules.StandardRules
import cnc.lexer.rules.TrieRule
import cnc.linter.CNCLinter
import cnc.linter.CamelCaseValidator
import cnc.linter.LinterFactory
import cnc.linter.SnakeCaseValidator
import cnc.parser.Parser
import cnc.parser.expression.Associativity
import cnc.parser.expression.ExpressionBuilder
import cnc.parser.expression.OperatorDef
import cnc.printscript.parser.PrintScriptStatementRules
import cnc.printscript.LanguageVersion
import cnc.printscript.profile.InterpreterIO
import cnc.printscript.profile.PrintScriptProfile
import cnc.semantic.ExpressionTypeResolver
import cnc.semantic.SemanticAnalyzer
import cnc.semantic.StandardExpressionTypeRules
import cnc.semantic.SymbolTable
import cnc.semantic.TypeResolvers
import cnc.token.CncKeywords
import cnc.token.CncPatterns
import cnc.token.CncSymbols
import cnc.token.TokenType

object PrintScript10Profile : PrintScriptProfile {
    override val version: LanguageVersion = LanguageVersion.V1_0

    fun createLexer(keywords: Map<String, TokenType> = CncKeywords.v1_0): Lexer {
        val rules = listOf(
            StandardRules.whitespace(),
            StandardRules.doubleQuotedString(TokenType.STRING),
            StandardRules.decimalNumber(TokenType.NUMBER),
            StandardRules.standardIdentifier(keywords = keywords),
            TrieRule(CncSymbols.all)
        )
        return Lexer(rules)
    }

    override fun createLexer(): Lexer = createLexer(CncKeywords.v1_0)

    override fun createParser(): Parser {
        val expressionBuilder = ExpressionBuilder()
            .addRecipes(mapOf(
                CncPatterns.NUMBER to { token -> NumberLiteral(token.text.toDouble()) },
                CncPatterns.STRING to { token -> StringLiteral(token.text.removeSurrounding("\"")) },
                CncPatterns.IDENTIFIER to { token -> Identifier(token.text) }
            ))
            .addOperators(listOf(
                OperatorDef(CncSymbols.PLUS, precedence = 1),
                OperatorDef(CncSymbols.MINUS, precedence = 1),
                OperatorDef(CncSymbols.MULTIPLICATION, precedence = 2),
                OperatorDef(CncSymbols.DIVISION, precedence = 2),
                OperatorDef(CncSymbols.EXPONENT, precedence = 3, associativity = Associativity.RIGHT)
            ))
        return Parser(
            rules = PrintScriptStatementRules.v1_0,
            expressionParser = expressionBuilder,
            blockOpen = CncSymbols.OPEN_BRACE,
            blockClose = CncSymbols.CLOSE_BRACE
        )
    }

    fun createBuiltins(io: InterpreterIO = InterpreterIO()): Map<String, cnc.common.BuiltinMethod> = mapOf(
        "println" to BuiltinMethodFactory.println(io.output)
    )

    fun createExpressionResolver(builtins: Map<String, cnc.common.BuiltinMethod> = createBuiltins()): ExpressionTypeResolver =
        ExpressionTypeResolver(
            rules = StandardExpressionTypeRules.v1_0,
            binaryRules = mapOf(
                "+" to TypeResolvers.additionOrConcat,
                "-" to TypeResolvers.numericOnly("-"),
                "*" to TypeResolvers.numericOnly("*"),
                "/" to TypeResolvers.numericOnly("/"),
                "**" to TypeResolvers.numericOnly("**")
            ),
            unaryRules = mapOf(
                "-" to TypeResolvers.unaryNumeric("-")
            ),
            builtins = builtins
        )

    override fun createSemanticAnalyzer(): SemanticAnalyzer =
        SemanticAnalyzer(
            symbolTable = SymbolTable(validTypes = setOf("number", "string")),
            expressionResolver = createExpressionResolver(),
            statementValidators = cnc.semantic.StandardStatementValidators.v1_0
        )

    fun createInterpreterBuilder(io: InterpreterIO): InterpreterBuilder {
        val builtins = createBuiltins(io)
        return Interpreter.builder()
            .registerEvaluators(mapOf(
                Declaration::class to DeclarationEvaluator(),
                Assignment::class to AssignmentEvaluator(),
                Call::class to CallEvaluator(builtins)
            ))
            .registerBinaryOperations(mapOf(
                "+" to BinaryOperation(StandardBinaryOperations::add),
                "-" to BinaryOperation(StandardBinaryOperations::subtract),
                "*" to BinaryOperation(StandardBinaryOperations::multiply),
                "/" to BinaryOperation(StandardBinaryOperations::divide)
            ))
            .registerUnaryOperations(mapOf(
                "-" to UnaryOperation(StandardUnaryOperations::negate),
                "+" to UnaryOperation(StandardUnaryOperations::positive)
            ))
            .registerBuiltins(builtins)
    }

    override fun createInterpreter(io: InterpreterIO): Interpreter =
        createInterpreterBuilder(io).build()

    override fun createFormatter(configJson: String?): Formatter =
        FormatterFactory.create(configJson)

    override fun createLinter(configJson: String): CNCLinter {
        val validators = mapOf(
            "camelCase" to CamelCaseValidator(),
            "snake_case" to SnakeCaseValidator()
        )
        return when (val res = LinterFactory.build(configJson, validators)) {
            is cnc.common.Success -> res.data
            is cnc.common.Failure -> CNCLinter(emptyList())
        }
    }
}
