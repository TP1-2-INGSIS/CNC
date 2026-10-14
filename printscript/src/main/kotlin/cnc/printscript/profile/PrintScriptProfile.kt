package cnc.printscript.profile

import cnc.formatter.Formatter
import cnc.interpreter.Interpreter
import cnc.lexer.Lexer
import cnc.linter.CNCLinter
import cnc.parser.Parser
import cnc.printscript.LanguageVersion
import cnc.semantic.SemanticAnalyzer

data class InterpreterIO(
    val input: (String) -> String = { readln() },
    val envProvider: (String) -> String? = System::getenv,
    val output: (String) -> Unit = ::println,
)

interface PrintScriptProfile {
    val version: LanguageVersion

    fun createLexer(): Lexer

    fun createParser(): Parser

    fun createSemanticAnalyzer(): SemanticAnalyzer

    fun createInterpreter(io: InterpreterIO = InterpreterIO()): Interpreter

    fun createFormatter(configJson: String? = null): Formatter

    fun createLinter(configJson: String): CNCLinter
}
