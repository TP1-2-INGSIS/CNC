package cnc.printscript.profile.v1_1

import cnc.ast.Assignment
import cnc.ast.BlockStatement
import cnc.ast.BooleanLiteral
import cnc.ast.Call
import cnc.ast.Declaration
import cnc.ast.Expression
import cnc.ast.IfStatement
import cnc.formatter.Formatter
import cnc.formatter.FormatterFactory
import cnc.interpreter.AssignmentEvaluator
import cnc.interpreter.BinaryOperation
import cnc.interpreter.BlockEvaluator
import cnc.interpreter.BuiltinMethodFactory
import cnc.interpreter.CallEvaluator
import cnc.interpreter.DeclarationEvaluator
import cnc.interpreter.IfEvaluator
import cnc.interpreter.Interpreter
import cnc.interpreter.StandardBinaryOperations
import cnc.interpreter.StandardUnaryOperations
import cnc.interpreter.UnaryOperation
import cnc.lexer.Lexer
import cnc.lexer.rules.StandardRules
import cnc.lexer.rules.TrieRule
import cnc.linter.CNCLinter
import cnc.parser.Parser
import cnc.parser.expression.ExpressionBuilder
import cnc.printscript.parser.PrintScriptStatementRules
import cnc.printscript.LanguageVersion
import cnc.printscript.profile.InterpreterIO
import cnc.printscript.profile.PrintScriptProfile
import cnc.printscript.profile.v1_0.PrintScript10Profile
import cnc.semantic.DefaultSemanticContext
import cnc.semantic.SemanticAnalyzer
import cnc.semantic.StandardExpressionTypeRules
import cnc.semantic.SymbolTable
import cnc.semantic.TypeResolvers
import cnc.token.CncKeywords
import cnc.token.CncSymbols
import cnc.token.SymbolTokenDef
import cnc.token.Token
import cnc.token.TokenDefinition
import cnc.token.TokenType

object PrintScript11Profile : PrintScriptProfile {
    override val version: LanguageVersion = LanguageVersion.V1_1

    override fun createLexer(): Lexer {
        val rules = listOf(
            StandardRules.whitespace(),
            StandardRules.doubleQuotedString(TokenType.STRING),
            StandardRules.decimalNumber(TokenType.NUMBER),
            StandardRules.standardIdentifier(keywords = CncKeywords.v1_1),
            TrieRule(CncSymbols.all)
        )
        return Lexer(rules)
    }

    override fun createParser(): Parser {
        val baseParser = PrintScript10Profile.createParser()
        val baseExprBuilder = baseParser.expressionParser as ExpressionBuilder
        val booleanRecipes: Map<TokenDefinition, (Token) -> Expression> = mapOf(
            SymbolTokenDef("true", "true") to { _ -> BooleanLiteral(true) },
            SymbolTokenDef("false", "false") to { _ -> BooleanLiteral(false) }
        )
        val expressionBuilder = baseExprBuilder.copy(
            recipes = booleanRecipes + baseExprBuilder.recipes
        )
        return baseParser.copy(
            rules = PrintScriptStatementRules.v1_1,
            expressionParser = expressionBuilder
        )
    }

    override fun createSemanticAnalyzer(): SemanticAnalyzer {
        val symbolTable = SymbolTable(validTypes = setOf("number", "string", "boolean"))
        val binaryRules = mapOf(
            "+" to TypeResolvers.additionOrConcat,
            "-" to TypeResolvers.numericOnly("-"),
            "*" to TypeResolvers.numericOnly("*"),
            "/" to TypeResolvers.numericOnly("/")
        )
        val unaryRules = mapOf(
            "-" to TypeResolvers.unaryNumeric("-")
        )
        val context = DefaultSemanticContext(
            symbolTable = symbolTable,
            binaryRules = binaryRules,
            unaryRules = unaryRules,
            expressionRules = StandardExpressionTypeRules.v1_1
        )
        return SemanticAnalyzer(context)
    }

    override fun createInterpreter(io: InterpreterIO): Interpreter {
        val builtins = mapOf(
            "println" to BuiltinMethodFactory.println(io.output),
            "readInput" to BuiltinMethodFactory.readInput(io.input),
            "readEnv" to BuiltinMethodFactory.readEnv(io.envProvider)
        )
        return Interpreter.builder()
            .registerEvaluators(mapOf(
                Declaration::class to DeclarationEvaluator(),
                Assignment::class to AssignmentEvaluator(),
                Call::class to CallEvaluator(builtins),
                BlockStatement::class to BlockEvaluator(),
                IfStatement::class to IfEvaluator()
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
            .build()
    }

    override fun createFormatter(configJson: String?): Formatter =
        FormatterFactory.create(configJson)

    override fun createLinter(configJson: String): CNCLinter =
        PrintScript10Profile.createLinter(configJson)
}
