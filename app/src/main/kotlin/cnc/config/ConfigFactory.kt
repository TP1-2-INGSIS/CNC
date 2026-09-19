package cnc.config

import cnc.Config
import cnc.interpreter.InterpreterPresets
import cnc.lexer.Lexer
import cnc.lexer.rules.StandardRules
import cnc.lexer.rules.TrieRule
import cnc.parser.Parser
import cnc.parser.rule.StandardStatementRules
import cnc.semantic.DefaultSemanticContext
import cnc.semantic.SemanticAnalyzer
import cnc.semantic.StandardExpressionTypeRules
import cnc.semantic.SymbolTable
import cnc.token.CncKeywords
import cnc.token.CncSymbols
import cnc.token.TokenType

object ConfigFactory {

    fun create(
        input: (String) -> String = { readln() },
        envProvider: (String) -> String? = System::getenv,
        output: (String) -> Unit = ::println
    ): Config {
        val lexerRules = listOf(
            StandardRules.whitespace(),
            StandardRules.doubleQuotedString(TokenType.STRING),
            StandardRules.decimalNumber(TokenType.NUMBER),
            StandardRules.standardIdentifier(keywords = CncKeywords.all),
            TrieRule(CncSymbols.all)
        )
        val lexer = Lexer(lexerRules)

        val parser = Parser(StandardStatementRules.v1_1, expressionBuilder)

        val symbolTable = SymbolTable(validTypes = setOf("number", "string", "boolean"))
        val semanticContext = DefaultSemanticContext(
            symbolTable = symbolTable,
            binaryRules = binaryTypeRules,
            unaryRules = unaryTypeRules,
            expressionRules = StandardExpressionTypeRules.v1_1
        )
        val semantic = SemanticAnalyzer(semanticContext)

        val interpreter = InterpreterPresets.default(input, envProvider, output)

        return Config(lexer, parser, semantic, interpreter)
    }
}
