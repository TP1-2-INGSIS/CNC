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
        version: LanguageVersion = LanguageVersion.V1_1,    // <-- TODO: buscar forma de que tome siempre 'latest' 
        input: (String) -> String = { readln() },
        envProvider: (String) -> String? = System::getenv,
        output: (String) -> Unit = ::println
    ): Config {
        val lexerRules = listOf(
            StandardRules.whitespace(),
            StandardRules.doubleQuotedString(TokenType.STRING),
            StandardRules.decimalNumber(TokenType.NUMBER),
            StandardRules.standardIdentifier(keywords = keywordsFor(version)),
            TrieRule(CncSymbols.all)
        )
        val lexer = Lexer(lexerRules)

        val parser = Parser(statementRulesFor(version), expressionBuilder)

        val symbolTable = SymbolTable(validTypes = validTypesFor(version))
        val semanticContext = DefaultSemanticContext(
            symbolTable = symbolTable,
            binaryRules = binaryTypeRules,
            unaryRules = unaryTypeRules,
            expressionRules = expressionRulesFor(version)
        )
        val semantic = SemanticAnalyzer(semanticContext)

        val interpreter = InterpreterPresets.default(input, envProvider, output)

        return Config(lexer, parser, semantic, interpreter)
    }

    private fun keywordsFor(version: LanguageVersion): Map<String, TokenType> = when (version) {
        LanguageVersion.V1_0 -> CncKeywords.v1_0
        LanguageVersion.V1_1 -> CncKeywords.v1_1
    }

    private fun statementRulesFor(version: LanguageVersion) = when (version) {
        LanguageVersion.V1_0 -> StandardStatementRules.v1_0
        LanguageVersion.V1_1 -> StandardStatementRules.v1_1
    }

    private fun expressionRulesFor(version: LanguageVersion) = when (version) {
        LanguageVersion.V1_0 -> StandardExpressionTypeRules.v1_0
        LanguageVersion.V1_1 -> StandardExpressionTypeRules.v1_1
    }

    private fun validTypesFor(version: LanguageVersion): Set<String> = when (version) {
        LanguageVersion.V1_0 -> setOf("number", "string")
        LanguageVersion.V1_1 -> setOf("number", "string", "boolean")
    }
}
