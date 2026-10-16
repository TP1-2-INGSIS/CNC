package cnc.language

import cnc.formatter.Formatter
import cnc.interpreter.Interpreter
import cnc.lexer.Lexer
import cnc.linter.CNCLinter
import cnc.parser.Parser
import cnc.semantic.SemanticAnalyzer

/** I/O channels injected into the interpreter (built-ins such as `println`, `readInput`, `readEnv`). */
data class InterpreterIO(
    val input: (String) -> String = { readln() },
    val envProvider: (String) -> String? = System::getenv,
    val output: (String) -> Unit = ::println,
)

/**
 * Factory of every pipeline component for a specific language [version].
 * A language (or a version of it) is defined by implementing this interface.
 */
interface LanguageProfile {
    val version: LanguageVersion

    fun createLexer(): Lexer

    fun createParser(): Parser

    fun createSemanticAnalyzer(): SemanticAnalyzer

    fun createInterpreter(io: InterpreterIO = InterpreterIO()): Interpreter

    fun createFormatter(configJson: String? = null): Formatter

    fun createLinter(configJson: String): CNCLinter
}
