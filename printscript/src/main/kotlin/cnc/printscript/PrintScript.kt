package cnc.printscript

import cnc.ast.Statement
import cnc.common.ContentManager
import cnc.common.Failure
import cnc.common.Result
import cnc.common.Success
import cnc.common.openStream
import cnc.formatter.FormatterFactory
import cnc.interpreter.Environment
import cnc.language.InterpreterIO
import cnc.language.LanguageFacade
import cnc.language.LanguageProfile
import cnc.printscript.profile.PrintScriptProfileRegistry
import cnc.token.Token

/**
 * Version-specific engine implementation of [LanguageFacade].
 * Backed by a [LanguageProfile] resolved from the [PrintScriptVersion].
 */
internal class PrintScriptFacade(
    version: PrintScriptVersion,
) : LanguageFacade {
    private val profile: LanguageProfile = PrintScriptProfileRegistry.forVersion(version)
    private val lexer = profile.createLexer()
    private val parser = profile.createParser()

    // =========================================================================
    // Granular Methods — Direct Access to Each Pipeline Stage
    // =========================================================================

    override fun lex(content: ContentManager): Sequence<Token> = lexer.tokenize(content.openStream())

    override fun parse(tokens: Sequence<Token>): Result<List<Statement>> {
        val statements = mutableListOf<Statement>()
        for (result in parser.parse(tokens)) {
            when (result) {
                is Failure -> return Failure(result.msg, result.type)
                is Success -> statements.add(result.data)
            }
        }
        return Success("ok", statements)
    }

    override fun semantic(statements: List<Statement>): Result<List<Statement>> {
        val semanticAnalyzer = profile.createSemanticAnalyzer()
        val validated = mutableListOf<Statement>()
        for (result in semanticAnalyzer.analyze(statements.asSequence())) {
            when (result) {
                is Failure -> return Failure(result.msg, result.type)
                is Success -> validated.add(result.data)
            }
        }
        return Success("ok", validated)
    }

    override fun interpret(
        statements: List<Statement>,
        io: InterpreterIO,
    ): Result<Unit> {
        val interpreter = profile.createInterpreter(io)
        val environment = Environment()
        return interpreter.interpret(statements, environment)
    }

    // =========================================================================
    // High-Level Operations — Full Pipeline in a Single Call
    // =========================================================================

    override fun execute(
        content: ContentManager,
        io: InterpreterIO,
    ): Result<Unit> {
        val tokens = lex(content)
        val semanticAnalyzer = profile.createSemanticAnalyzer()
        val interpreter = profile.createInterpreter(io)
        val environment = Environment()

        for (parseResult in parser.parse(tokens)) {
            val statement =
                when (parseResult) {
                    is Failure -> return Failure(parseResult.msg, parseResult.type)
                    is Success -> parseResult.data
                }

            for (semanticResult in semanticAnalyzer.analyze(sequenceOf(statement))) {
                if (semanticResult is Failure) {
                    return Failure(semanticResult.msg, semanticResult.type)
                }
            }

            val interpretResult = interpreter.interpret(statement, environment)
            if (interpretResult is Failure) {
                return Failure(interpretResult.msg, interpretResult.type)
            }
        }
        return Success("ok", Unit)
    }

    override fun validate(content: ContentManager): Result<Unit> {
        val tokens = lex(content)
        val semanticAnalyzer = profile.createSemanticAnalyzer()

        for (parseResult in parser.parse(tokens)) {
            val statement =
                when (parseResult) {
                    is Failure -> return Failure(parseResult.msg, parseResult.type)
                    is Success -> parseResult.data
                }

            for (semanticResult in semanticAnalyzer.analyze(sequenceOf(statement))) {
                if (semanticResult is Failure) {
                    return Failure(semanticResult.msg, semanticResult.type)
                }
            }
        }
        return Success("Code is valid", Unit)
    }

    override fun format(
        content: ContentManager,
        configJson: String?,
    ): String =
        if (configJson.isNullOrBlank()) {
            val parseResult = parse(lex(content))
            val statements = if (parseResult is Success) parseResult.data else emptyList()
            profile.createFormatter(null).format(statements)
        } else {
            val text = content.getReader().use { it.readText() }
            FormatterFactory.formatSource(text, configJson)
        }

    override fun lint(
        content: ContentManager,
        configJson: String,
    ): List<String> =
        when (val parseResult = parse(lex(content))) {
            is Failure -> listOf("Syntax error: ${parseResult.msg}")
            is Success -> profile.createLinter(configJson).lint(parseResult.data.asSequence())
        }
}
