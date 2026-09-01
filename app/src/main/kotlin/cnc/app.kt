package cnc

import cnc.config.*
import cnc.lexer.Lexer
import cnc.common.StrContent
import cnc.common.ContentManager
import cnc.common.Success
import cnc.common.Failure
import cnc.parser.Parser
import cnc.semantic.SemanticAnalyzer

data class Config(
  val lexer: Lexer = printScriptLexer,
  val parser: Parser = Parser(grammars, terminators),
  val semantic: SemanticAnalyzer = SemanticAnalyzer(semanticContext)
)

data class Compiler(
  val config: Config
) {
  fun compile(content: ContentManager) {
    val tokens = config.lexer.tokenize(content)
    val statements = config.parser.getASTs(tokens)
    config.semantic.analyze(statements).forEach { result ->
      when (result) {
        is Success -> println("OK: ${result.data}")
        is Failure -> println("ERROR: ${result.msg}")
      }
    }
  }
}

fun main() {
  val compiler = Compiler(Config())
  compiler.compile(StrContent("""
    let x: number = 10;
    let y: number = 20;
    let sum: number = x + y;
    let product: number = x * y + 2;
    let complex: number = (x + y) * (x - y);
    let name: string = "hello";
    let greeting: string = "world";
    let concat: string = name + greeting;
    let bad: string = x + y;
    let duplicate: number = 1;
    let duplicate: number = 2;
    sum = x + y * 3;
    sum = "oops";
    undeclared = 5;
    println(x);
    println(sum + product);
    println(name);
  """.trimIndent()))
}
