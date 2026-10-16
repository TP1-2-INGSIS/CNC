package cnc.language

import cnc.ast.Statement
import cnc.common.ContentManager
import cnc.common.Result
import cnc.token.Token

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

    /** Executes an already parsed AST. */
    fun interpret(
        statements: List<Statement>,
        output: (String) -> Unit = ::println,
        input: (String) -> String = { readln() },
        envProvider: (String) -> String? = System::getenv,
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
        input: (String) -> String = { readln() },
        envProvider: (String) -> String? = System::getenv,
        output: (String) -> Unit = ::println,
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
 * [LanguageHighOperations] into a single contract. Callers that start from a
 * [String], [java.io.File] or [java.io.InputStream] wrap it in a
 * [ContentManager] (`StringContent`, `FileContent`, `InputStreamContent`).
 */
interface LanguageFacade :
    LanguageCore,
    LanguageHighOperations
