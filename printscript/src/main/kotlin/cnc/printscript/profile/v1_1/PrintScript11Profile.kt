package cnc.printscript.profile.v1_1

import cnc.ast.BlockStatement
import cnc.ast.BooleanLiteral
import cnc.ast.Call
import cnc.ast.CallExpression
import cnc.ast.Expression
import cnc.ast.IfStatement
import cnc.ast.Statement
import cnc.formatter.Formatter
import cnc.formatter.FormatterFactory
import cnc.interpreter.BlockEvaluator
import cnc.interpreter.BuiltinMethodFactory
import cnc.interpreter.CallEvaluator
import cnc.interpreter.IfEvaluator
import cnc.interpreter.Interpreter
import cnc.lexer.Lexer
import cnc.linter.CNCLinter
import cnc.parser.Parser
import cnc.parser.expression.ExpressionBuilder
import cnc.printscript.parser.PrintScriptStatementRules
import cnc.printscript.LanguageVersion
import cnc.printscript.profile.InterpreterIO
import cnc.printscript.profile.PrintScriptProfile
import cnc.printscript.profile.v1_0.PrintScript10Profile
import cnc.semantic.ExpressionRule
import cnc.semantic.SemanticAnalyzer
import cnc.semantic.StandardExpressionRules
import cnc.semantic.StandardStatementValidators
import cnc.semantic.StatementValidator
import cnc.semantic.SymbolTable
import cnc.token.CncKeywords
import cnc.token.SymbolTokenDef
import kotlin.reflect.KClass

object PrintScript11Profile : PrintScriptProfile {
    override val version: LanguageVersion = LanguageVersion.V1_1

    override fun createLexer(): Lexer =
        PrintScript10Profile.createLexer(CncKeywords.v1_1)

    override fun createParser(): Parser {
        val baseParser = PrintScript10Profile.createParser()
        val baseExprBuilder = baseParser.expressionParser as ExpressionBuilder
        val expressionBuilder = baseExprBuilder.copy()
            .addRecipeFirst(SymbolTokenDef("true", "true")) { _ -> BooleanLiteral(true) }
            .addRecipeFirst(SymbolTokenDef("false", "false")) { _ -> BooleanLiteral(false) }
        return baseParser.copy(
            rules = PrintScriptStatementRules.v1_1,
            expressionParser = expressionBuilder
        )
    }

    fun createBuiltins(io: InterpreterIO = InterpreterIO()): Map<String, cnc.common.BuiltinMethod> =
        PrintScript10Profile.createBuiltins(io) + mapOf(
            "readInput" to BuiltinMethodFactory.readInput(io.input),
            "readEnv" to BuiltinMethodFactory.readEnv(io.envProvider)
        )

    fun createExpressionRules(): Map<KClass<out Expression>, ExpressionRule<out Expression>> =
        PrintScript10Profile.createExpressionRules() + mapOf(
            BooleanLiteral::class to StandardExpressionRules.booleanLiteral,
            CallExpression::class to StandardExpressionRules.callExpression
        )

    fun createStatementValidators(): Map<KClass<out Statement>, StatementValidator<*>> =
        PrintScript10Profile.createStatementValidators() + mapOf(
            BlockStatement::class to StandardStatementValidators.block,
            IfStatement::class to StandardStatementValidators.ifValidator
        )

    override fun createSemanticAnalyzer(): SemanticAnalyzer {
        val builtins = createBuiltins()
        val baseResolver = PrintScript10Profile.createExpressionResolver(builtins)
        val expressionResolver = cnc.semantic.ExpressionTypeResolver(
            rules = createExpressionRules(),
            binaryRules = baseResolver.binaryRules,
            unaryRules = baseResolver.unaryRules + mapOf("!" to cnc.semantic.TypeResolvers.booleanNot),
            builtins = builtins
        )
        return SemanticAnalyzer(
            symbolTable = SymbolTable(validTypes = setOf("number", "string", "boolean")),
            expressionResolver = expressionResolver,
            statementValidators = createStatementValidators()
        )
    }

    override fun createInterpreter(io: InterpreterIO): Interpreter {
        val builtins = createBuiltins(io)
        return PrintScript10Profile.createInterpreterBuilder(io)
            .registerBuiltins(builtins)
            .registerEvaluator(Call::class, CallEvaluator(builtins))
            .registerEvaluator(BlockStatement::class, BlockEvaluator())
            .registerEvaluator(IfStatement::class, IfEvaluator())
            .build()
    }

    override fun createFormatter(configJson: String?): Formatter =
        FormatterFactory.create(configJson)

    override fun createLinter(configJson: String): CNCLinter =
        PrintScript10Profile.createLinter(configJson)
}
