package cnc.language

import cnc.ast.Statement
import cnc.common.ContentManager
import cnc.common.Result
import cnc.common.StringContent
import cnc.common.Success
import cnc.formatter.Formatter
import cnc.formatter.FormatterFactory
import cnc.interpreter.Interpreter
import cnc.lexer.Lexer
import cnc.linter.CNCLinter
import cnc.parser.Parser
import cnc.semantic.SemanticAnalyzer
import cnc.token.Token
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class LanguageContractsTest {
    private enum class FakeVersion(
        override val label: String,
    ) : LanguageVersion {
        V1("1"),
    }

    /** Records what the facade receives so default arguments can be asserted. */
    private class RecordingFacade : LanguageFacade {
        var interpretOutput: ((String) -> Unit)? = null
        var executeInput: ((String) -> String)? = null
        var formatConfig: String? = "unset"

        override fun lex(content: ContentManager): Sequence<Token> = emptySequence()

        override fun parse(tokens: Sequence<Token>): Result<List<Statement>> = Success("ok", emptyList())

        override fun semantic(statements: List<Statement>): Result<List<Statement>> = Success("ok", statements)

        override fun interpret(
            statements: List<Statement>,
            output: (String) -> Unit,
            input: (String) -> String,
            envProvider: (String) -> String?,
        ): Result<Unit> {
            interpretOutput = output
            return Success("ok", Unit)
        }

        override fun execute(
            content: ContentManager,
            input: (String) -> String,
            envProvider: (String) -> String?,
            output: (String) -> Unit,
        ): Result<Unit> {
            executeInput = input
            return Success("ok", Unit)
        }

        override fun validate(content: ContentManager): Result<Unit> = Success("ok", Unit)

        override fun format(
            content: ContentManager,
            configJson: String?,
        ): String {
            formatConfig = configJson
            return ""
        }

        override fun lint(
            content: ContentManager,
            configJson: String,
        ): List<String> = emptyList()
    }

    /** Minimal profile; only the formatter is actually built. */
    private class FakeProfile : LanguageProfile {
        var interpreterIO: InterpreterIO? = null
        var formatterConfig: String? = "unset"

        override val version: LanguageVersion = FakeVersion.V1

        override fun createLexer(): Lexer = Lexer(emptyList())

        override fun createParser(): Parser = error("not needed")

        override fun createSemanticAnalyzer(): SemanticAnalyzer = error("not needed")

        override fun createInterpreter(io: InterpreterIO): Interpreter {
            interpreterIO = io
            return Interpreter.builder().build()
        }

        override fun createFormatter(configJson: String?): Formatter {
            formatterConfig = configJson
            return FormatterFactory.create(configJson)
        }

        override fun createLinter(configJson: String): CNCLinter = CNCLinter(emptyList())
    }

    @Test
    fun `facade is usable through each segregated interface`() {
        val facade = RecordingFacade()
        val core: LanguageCore = facade
        val high: LanguageHighOperations = facade
        val content = StringContent("")

        assertTrue(core.parse(core.lex(content)).isOk())
        assertTrue(core.semantic(emptyList()).isOk())
        assertTrue(high.validate(content).isOk())
        assertTrue(high.lint(content, "{}").isEmpty())
    }

    @Test
    fun `facade default arguments are forwarded to the implementation`() {
        val facade = RecordingFacade()
        val content = StringContent("")

        assertTrue(facade.interpret(emptyList()).isOk())
        assertTrue(facade.execute(content).isOk())
        facade.format(content)

        assertTrue(facade.interpretOutput != null)
        assertTrue(facade.executeInput != null)
        assertNull(facade.formatConfig)
    }

    @Test
    fun `profile default arguments and version`() {
        val profile = FakeProfile()

        profile.createInterpreter()
        profile.createFormatter()
        profile.createLexer()
        profile.createLinter("{}")

        assertTrue(profile.interpreterIO != null)
        assertNull(profile.formatterConfig)
        assertEquals("1", profile.version.label)
    }

    @Test
    fun `interpreter io keeps custom channels`() {
        val out = mutableListOf<String>()
        val input: (String) -> String = { "typed" }
        val env: (String) -> String? = { if (it == "K") "V" else null }
        val output: (String) -> Unit = { out.add(it) }

        val io = InterpreterIO(input, env, output)
        io.output("hello")

        assertEquals("typed", io.input("prompt"))
        assertEquals("V", io.envProvider("K"))
        assertNull(io.envProvider("missing"))
        assertEquals(listOf("hello"), out)
        assertSame(output, io.copy().output)

        val defaults = InterpreterIO()
        assertNull(defaults.envProvider("CNC_SURELY_UNDEFINED_ENV_VAR"))
    }
}
