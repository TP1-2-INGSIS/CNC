package cnc.printscript

import java.io.File
import java.io.InputStream
import cnc.Compiler
import cnc.Config
import cnc.ast.Statement
import cnc.common.ContentManager
import cnc.common.Failure
import cnc.common.FileContent
import cnc.common.InputStreamContent
import cnc.common.Result
import cnc.common.StringContent
import cnc.common.Success
import cnc.common.flatMap
import cnc.common.openStream
import cnc.config.ConfigFactory
import cnc.config.LanguageVersion
import cnc.config.printScriptFormatter
import cnc.interpreter.Environment
import cnc.interpreter.InterpreterPresets
import cnc.linter.CamelCaseValidator
import cnc.linter.LinterFactory
import cnc.linter.NamingValidator
import cnc.linter.SnakeCaseValidator
import cnc.token.Token

/**
 * Public facade interface for the PrintScript language engine.
 * Allows consumers to interact with all compilation and execution stages
 * through a single unified contract.
 */
interface PrintScriptFacade {

    // =========================================================================
    // High-Level Operations (End-to-End Pipeline)
    // =========================================================================

    fun execute(
        source: String,
        input: (String) -> String = { readln() },
        envProvider: (String) -> String? = System::getenv,
        output: (String) -> Unit = ::println
    ): Result<Unit>

    fun execute(
        file: File,
        input: (String) -> String = { readln() },
        envProvider: (String) -> String? = System::getenv,
        output: (String) -> Unit = ::println
    ): Result<Unit>

    fun execute(
        stream: InputStream,
        input: (String) -> String = { readln() },
        envProvider: (String) -> String? = System::getenv,
        output: (String) -> Unit = ::println
    ): Result<Unit>

    fun execute(
        content: ContentManager,
        input: (String) -> String = { readln() },
        envProvider: (String) -> String? = System::getenv,
        output: (String) -> Unit = ::println
    ): Result<Unit>

    fun validate(source: String): Result<Unit>
    fun validate(file: File): Result<Unit>
    fun validate(stream: InputStream): Result<Unit>
    fun validate(content: ContentManager): Result<Unit>
    fun validate(statements: List<Statement>): Result<Unit>

    fun format(source: String): String
    fun format(file: File): String
    fun format(stream: InputStream): String
    fun format(content: ContentManager): String
    fun format(statements: List<Statement>): String

    fun lint(source: String, configJson: String): List<String>
    fun lint(file: File, configJson: String): List<String>
    fun lint(stream: InputStream, configJson: String): List<String>
    fun lint(content: ContentManager, configJson: String): List<String>
    fun lint(statements: List<Statement>, configJson: String): List<String>
    fun lint(source: String, configFile: File): List<String>
    fun lint(file: File, configFile: File): List<String>
    fun lint(stream: InputStream, configFile: File): List<String>
    fun lint(statements: List<Statement>, configFile: File): List<String>

    // =========================================================================
    // Granular Pipeline Stages
    // =========================================================================

    fun lex(source: String): Sequence<Token>
    fun lex(file: File): Sequence<Token>
    fun lex(stream: InputStream): Sequence<Token>
    fun lex(content: ContentManager): Sequence<Token>

    fun parse(tokens: Sequence<Token>): Result<List<Statement>>
    fun parse(source: String): Result<List<Statement>>
    fun parse(file: File): Result<List<Statement>>
    fun parse(stream: InputStream): Result<List<Statement>>
    fun parse(content: ContentManager): Result<List<Statement>>

    fun semantic(statements: List<Statement>): Result<List<Statement>>
    fun semantic(source: String): Result<List<Statement>>
    fun semantic(file: File): Result<List<Statement>>
    fun semantic(stream: InputStream): Result<List<Statement>>
    fun semantic(content: ContentManager): Result<List<Statement>>

    fun analyze(statements: List<Statement>): Result<List<Statement>>
    fun analyze(source: String): Result<List<Statement>>
    fun analyze(file: File): Result<List<Statement>>
    fun analyze(stream: InputStream): Result<List<Statement>>
    fun analyze(content: ContentManager): Result<List<Statement>>

    fun interpret(
        statements: List<Statement>,
        output: (String) -> Unit = ::println,
        input: (String) -> String = { readln() },
        envProvider: (String) -> String? = System::getenv
    ): Result<Unit>

    fun interpret(
        source: String,
        output: (String) -> Unit = ::println,
        input: (String) -> String = { readln() },
        envProvider: (String) -> String? = System::getenv
    ): Result<Unit>

    fun interpret(
        file: File,
        output: (String) -> Unit = ::println,
        input: (String) -> String = { readln() },
        envProvider: (String) -> String? = System::getenv
    ): Result<Unit>

    fun interpret(
        stream: InputStream,
        output: (String) -> Unit = ::println,
        input: (String) -> String = { readln() },
        envProvider: (String) -> String? = System::getenv
    ): Result<Unit>

    fun interpret(
        content: ContentManager,
        output: (String) -> Unit = ::println,
        input: (String) -> String = { readln() },
        envProvider: (String) -> String? = System::getenv
    ): Result<Unit>
}

/**
 * Singleton entry-point and facade for the PrintScript language library.
 * With a single import (`import cnc.printscript.PrintScript`), users have
 * access to every execution, validation, formatting, and linting capability.
 *
 * Version-specific engine implementation of [PrintScriptFacade].
 *
 * Instances are internal: consumers obtain a facade exclusively through
 * [PsVersioner.version], which selects the language version. Each instance
 * carries its own default [Config] built for its [LanguageVersion].
 */
internal class PrintScriptEngine(
    private val version: LanguageVersion
) : PrintScriptFacade {

    private val config: Config = ConfigFactory.create(version)

    // =========================================================================
    // Granular Methods — Direct Access to Each Pipeline Stage
    // =========================================================================

    override fun lex(content: ContentManager): Sequence<Token> {
        return config.lexer.tokenize(content.openStream())
    }
    override fun lex(source: String): Sequence<Token> = lex(StringContent(source))
    override fun lex(file: File): Sequence<Token> = lex(FileContent(file.absolutePath))
    override fun lex(stream: InputStream): Sequence<Token> = lex(InputStreamContent(stream))

    
    override fun parse(tokens: Sequence<Token>): Result<List<Statement>> {
        val statements = mutableListOf<Statement>()
        for (result in config.parser.parse(tokens)) {
            when (result) {
                is Failure -> return Failure(result.msg, result.type)
                is Success -> statements.add(result.data)
            }
        }
        return Success("ok", statements)
    }
    override fun parse(content: ContentManager): Result<List<Statement>> = parse(lex(content))
    override fun parse(source: String): Result<List<Statement>> = parse(StringContent(source))
    override fun parse(file: File): Result<List<Statement>> = parse(FileContent(file.absolutePath))
    override fun parse(stream: InputStream): Result<List<Statement>> = parse(InputStreamContent(stream))

    override fun semantic(statements: List<Statement>): Result<List<Statement>> {
        val semanticAnalyzer = ConfigFactory.create(version).semantic
        val validated = mutableListOf<Statement>()
        for (result in semanticAnalyzer.analyze(statements.asSequence())) {
            when (result) {
                is Failure -> return Failure(result.msg, result.type)
                is Success -> validated.add(result.data)
            }
        }
        return Success("ok", validated)
    }
    override fun semantic(content: ContentManager): Result<List<Statement>> =
        parse(content).flatMap { semantic(it) }
    override fun semantic(source: String): Result<List<Statement>> =
        semantic(StringContent(source))
    override fun semantic(file: File): Result<List<Statement>> =
        semantic(FileContent(file.absolutePath))
    override fun semantic(stream: InputStream): Result<List<Statement>> =
        semantic(InputStreamContent(stream))

    override fun analyze(statements: List<Statement>): Result<List<Statement>> = semantic(statements)
    override fun analyze(source: String): Result<List<Statement>> = semantic(source)
    override fun analyze(file: File): Result<List<Statement>> = semantic(file)
    override fun analyze(stream: InputStream): Result<List<Statement>> = semantic(stream)
    override fun analyze(content: ContentManager): Result<List<Statement>> = semantic(content)

    override fun interpret(
        statements: List<Statement>,
        output: (String) -> Unit,
        input: (String) -> String,
        envProvider: (String) -> String?
    ): Result<Unit> {
        val interpreter = InterpreterPresets.default(input, envProvider, output)
        val environment = Environment()
        return interpreter.interpret(statements, environment)
    }

    override fun interpret(
        content: ContentManager,
        output: (String) -> Unit,
        input: (String) -> String,
        envProvider: (String) -> String?
    ): Result<Unit> = execute(content, input, envProvider, output)

    override fun interpret(
        source: String,
        output: (String) -> Unit,
        input: (String) -> String,
        envProvider: (String) -> String?
    ): Result<Unit> = interpret(StringContent(source), output, input, envProvider)

    override fun interpret(
        file: File,
        output: (String) -> Unit,
        input: (String) -> String,
        envProvider: (String) -> String?
    ): Result<Unit> = interpret(FileContent(file.absolutePath), output, input, envProvider)

    override fun interpret(
        stream: InputStream,
        output: (String) -> Unit,
        input: (String) -> String,
        envProvider: (String) -> String?
    ): Result<Unit> = interpret(InputStreamContent(stream), output, input, envProvider)

    // =========================================================================
    // High-Level Operations — Full Pipeline in a Single Call
    // =========================================================================

    override fun execute(
        content: ContentManager,
        input: (String) -> String,
        envProvider: (String) -> String?,
        output: (String) -> Unit
    ): Result<Unit> {
        val cfg = ConfigFactory.create(version, input, envProvider, output)
        val compiler = Compiler(cfg)
        return compiler.execute(content)
    }

    override fun execute(
        source: String,
        input: (String) -> String,
        envProvider: (String) -> String?,
        output: (String) -> Unit
    ): Result<Unit> = execute(StringContent(source), input, envProvider, output)

    override fun execute(
        file: File,
        input: (String) -> String,
        envProvider: (String) -> String?,
        output: (String) -> Unit
    ): Result<Unit> = execute(FileContent(file.absolutePath), input, envProvider, output)

    override fun execute(
        stream: InputStream,
        input: (String) -> String,
        envProvider: (String) -> String?,
        output: (String) -> Unit
    ): Result<Unit> = execute(InputStreamContent(stream), input, envProvider, output)

    override fun validate(content: ContentManager): Result<Unit> {
        val compiler = Compiler(ConfigFactory.create(version))
        return compiler.validate(content)
    }

    override fun validate(source: String): Result<Unit> = validate(StringContent(source))
    override fun validate(file: File): Result<Unit> = validate(FileContent(file.absolutePath))
    override fun validate(stream: InputStream): Result<Unit> = validate(InputStreamContent(stream))

    override fun validate(statements: List<Statement>): Result<Unit> {
        val semanticResult = semantic(statements)
        return when (semanticResult) {
            is Failure -> Failure(semanticResult.msg, semanticResult.type)
            is Success -> Success("Code is valid", Unit)
        }
    }

    override fun format(statements: List<Statement>): String {
        return printScriptFormatter.format(statements)
    }

    override fun format(content: ContentManager): String {
        val parseResult = parse(content)
        val statements = if (parseResult is Success) parseResult.data else emptyList()
        return format(statements)
    }

    override fun format(source: String): String = format(StringContent(source))
    override fun format(file: File): String = format(FileContent(file.absolutePath))
    override fun format(stream: InputStream): String = format(InputStreamContent(stream))

    override fun lint(statements: List<Statement>, configJson: String): List<String> {
        val validators: Map<String, NamingValidator> = mapOf(
            "camelCase" to CamelCaseValidator(),
            "snake_case" to SnakeCaseValidator()
        )
        return when (val linterResult = LinterFactory.build(configJson, validators)) {
            is Failure -> listOf("Linter config error: ${linterResult.msg}")
            is Success -> linterResult.data.lint(statements.asSequence())
        }
    }

    override fun lint(content: ContentManager, configJson: String): List<String> {
        val parseResult = parse(content)
        return when (parseResult) {
            is Failure -> listOf("Syntax error: ${parseResult.msg}")
            is Success -> lint(parseResult.data, configJson)
        }
    }

    override fun lint(source: String, configJson: String): List<String> = lint(StringContent(source), configJson)
    override fun lint(file: File, configJson: String): List<String> = lint(FileContent(file.absolutePath), configJson)
    override fun lint(stream: InputStream, configJson: String): List<String> = lint(InputStreamContent(stream), configJson)
    override fun lint(statements: List<Statement>, configFile: File): List<String> = lint(statements, configFile.readText())
    override fun lint(source: String, configFile: File): List<String> = lint(source, configFile.readText())
    override fun lint(file: File, configFile: File): List<String> = lint(file, configFile.readText())
    override fun lint(stream: InputStream, configFile: File): List<String> = lint(stream, configFile.readText())
}
