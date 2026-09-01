package cnc.config

import cnc.lexer.Lexer
import cnc.lexer.rules.StandardRules
import cnc.lexer.rules.TrieRule

import cnc.token.TokenType

val printScriptRules = listOf(
  StandardRules.whitespace(),
  StandardRules.doubleQuotedString(TokenType.STRING),
  StandardRules.integerNumber(TokenType.NUMBER),
  StandardRules.standardIdentifier(keywords = CncKeywords.all),
  TrieRule(CncSymbols.all)
)

val printScriptLexer = Lexer(printScriptRules)
