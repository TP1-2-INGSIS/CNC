package cnc.language

import cnc.ast.Statement
import cnc.common.ContentManager
import cnc.common.Result
import cnc.token.Token
import java.io.File
import java.io.InputStream

/**
 * Granular pipeline stages. Each stage consumes the output of the previous one:
 * a text source is turned into tokens through [lex], tokens into statements
 * through [parse], and statements are then analyzed or executed.
 */
interface LanguageCore {
    /** Reads a source into a lazy stream of tokens. */
    fun lex(content: ContentManager): Sequence<Token>

    /** Builds the AST from a token stream. */
    fun parse(tokens: Sequence<Token>): Result<List<Statement>>

    /** Type-checks and validates an already parsed AST. */
    fun semantic(statements: List<Statement>): Result<List<Statement>>

    /** Executes an already parsed AST using the given I/O channels. */
    fun interpret(
        statements: List<Statement>,
        io: InterpreterIO = InterpreterIO(),
    ): Result<Unit>
}

/**
 * High-level, end-to-end operations over a source. Each method wraps the full
 * pipeline (lex + parse + semantic + ...) in a single call.
 */
interface LanguageHighOperations {
    /** Runs the full pipeline over a source: lex + parse + semantic + interpret. */
    fun execute(
        content: ContentManager,
        io: InterpreterIO = InterpreterIO(),
    ): Result<Unit>

    /** Checks syntax and semantics without executing. */
    fun validate(content: ContentManager): Result<Unit>

    /** Formats a source into its canonical representation. */
    fun format(
        content: ContentManager,
        configJson: String? = null,
    ): String

    /** Lints a source against the given configuration. */
    fun lint(
        content: ContentManager,
        configJson: String,
    ): List<String>
}

/**
 * Public facade for a language engine.
 *
 * Composes the granular [LanguageCore] stages with the end-to-end
 * [LanguageHighOperations] into a single contract.
 *
 * The core contract is expressed in terms of [ContentManager], an internal
 * reading abstraction. Callers that start from raw, canonical sources
 * ([String], [java.io.File] or [java.io.InputStream]) should prefer the
 * convenience extensions declared below, which hide [ContentManager] entirely.
 */
interface LanguageFacade :
    LanguageCore,
    LanguageHighOperations

// =============================================================================
// Convenience extensions — operate on raw, canonical sources without forcing
// callers to know about the internal `ContentManager` reading abstraction.
// =============================================================================

/** Executes source [code] provided as a raw [String]. */
fun LanguageFacade.execute(
    code: String,
    io: InterpreterIO = InterpreterIO(),
): Result<Unit> = execute(ContentManager.of(code), io)

/** Executes the source read from [file]. */
fun LanguageFacade.execute(
    file: File,
    io: InterpreterIO = InterpreterIO(),
): Result<Unit> = execute(ContentManager.of(file), io)

/** Executes the source read from [stream]. */
fun LanguageFacade.execute(
    stream: InputStream,
    io: InterpreterIO = InterpreterIO(),
): Result<Unit> = execute(ContentManager.of(stream), io)

/** Validates source [code] provided as a raw [String]. */
fun LanguageFacade.validate(code: String): Result<Unit> = validate(ContentManager.of(code))

/** Validates the source read from [file]. */
fun LanguageFacade.validate(file: File): Result<Unit> = validate(ContentManager.of(file))

/** Validates the source read from [stream]. */
fun LanguageFacade.validate(stream: InputStream): Result<Unit> = validate(ContentManager.of(stream))

/** Formats source [code] provided as a raw [String]. */
fun LanguageFacade.format(
    code: String,
    configJson: String? = null,
): String = format(ContentManager.of(code), configJson)

/** Formats the source read from [file]. */
fun LanguageFacade.format(
    file: File,
    configJson: String? = null,
): String = format(ContentManager.of(file), configJson)

/** Lints source [code] provided as a raw [String]. */
fun LanguageFacade.lint(
    code: String,
    configJson: String,
): List<String> = lint(ContentManager.of(code), configJson)

/** Lints the source read from [file]. */
fun LanguageFacade.lint(
    file: File,
    configJson: String,
): List<String> = lint(ContentManager.of(file), configJson)
