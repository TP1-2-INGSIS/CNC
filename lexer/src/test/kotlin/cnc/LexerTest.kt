package cnc.lexer

import cnc.common.Position
import cnc.common.StrContent
import cnc.token.RegexTokenDef
import cnc.token.SymbolTokenDef
import cnc.token.TokenDefinition
import cnc.token.TokenDefinitionProvider
import cnc.token.TokenType
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

// ---------------------------------------------------------------------------
// TokenDefinitionProvider de prueba — replica la config del lenguaje
// sin depender del módulo :app
// ---------------------------------------------------------------------------
private object TestTokenDefs : TokenDefinitionProvider {
  private val definitions =
    mapOf(
      TokenType.OPERATOR to
        listOf(
          SymbolTokenDef(TokenType.OPERATOR, listOf("+", "-", "/", "*", "==")),
        ),
      TokenType.SYMBOL to
        listOf(
          SymbolTokenDef(TokenType.SYMBOL, listOf(";", ":", "=")),
        ),
      TokenType.VARIABLE_TYPE to
        listOf(
          SymbolTokenDef(TokenType.VARIABLE_TYPE, listOf("string", "number")),
        ),
      TokenType.KEYWORD to
        listOf(
          SymbolTokenDef(TokenType.KEYWORD, "let"),
        ),
      TokenType.IDENTIFIER to
        listOf(
          RegexTokenDef(TokenType.IDENTIFIER, "[a-zA-Z_][a-zA-Z0-9_]*"),
        ),
      TokenType.NUMBER to
        listOf(
          RegexTokenDef(TokenType.NUMBER, "[0-9]+"),
        ),
      TokenType.STRING to
        listOf(
          RegexTokenDef(TokenType.STRING, "\".*?\""),
        ),
    )

  override fun getValue(type: TokenType): List<TokenDefinition>? = definitions[type]

  override fun getTypes(): Set<TokenType> = definitions.keys

  override fun type(str: String): TokenType {
    for (type in getTypes()) {
      for (def in definitions[type]!!) {
        if (def.match(str)) return type
      }
    }
    return TokenType.INVALID
  }
}

// ---------------------------------------------------------------------------
// Suite principal
// ---------------------------------------------------------------------------
class LexerTest {
  private val lexer = Lexer(TestTokenDefs)

  private fun lex(input: String) = lexer.getTokens(StrContent(input)).toList()

  // -------------------------------------------------------------------------
  // TokenType — cada token es reconocido con el tipo correcto
  // -------------------------------------------------------------------------

  @Nested
  inner class TokenTypes {
    @Test fun `let es KEYWORD`() {
      val tokens = lex("let")
      assertEquals(1, tokens.size)
      assertEquals(TokenType.KEYWORD, tokens[0].type)
    }

    @Test fun `identificador simple es IDENTIFIER`() {
      val tokens = lex("myVar")
      assertEquals(1, tokens.size)
      assertEquals(TokenType.IDENTIFIER, tokens[0].type)
    }

    @Test fun `identificador con guion bajo es IDENTIFIER`() {
      val tokens = lex("_my_var_2")
      assertEquals(1, tokens.size)
      assertEquals(TokenType.IDENTIFIER, tokens[0].type)
    }

    @Test fun `string (tipo) es VARIABLE_TYPE`() {
      assertEquals(TokenType.VARIABLE_TYPE, lex("string")[0].type)
    }

    @Test fun `number (tipo) es VARIABLE_TYPE`() {
      assertEquals(TokenType.VARIABLE_TYPE, lex("number")[0].type)
    }

    @Test fun `literal entero es NUMBER`() {
      val tokens = lex("42")
      assertEquals(1, tokens.size)
      assertEquals(TokenType.NUMBER, tokens[0].type)
    }

    @Test fun `literal de string es STRING`() {
      val tokens = lex("\"hello world\"")
      assertEquals(1, tokens.size)
      assertEquals(TokenType.STRING, tokens[0].type)
    }

    @Test fun `operador suma es OPERATOR`() {
      assertEquals(TokenType.OPERATOR, lex("+")[0].type)
    }

    @Test fun `operador resta es OPERATOR`() {
      assertEquals(TokenType.OPERATOR, lex("-")[0].type)
    }

    @Test fun `operador multiplicacion es OPERATOR`() {
      assertEquals(TokenType.OPERATOR, lex("*")[0].type)
    }

    @Test fun `operador division es OPERATOR`() {
      assertEquals(TokenType.OPERATOR, lex("/")[0].type)
    }

    @Test fun `operador igualdad es OPERATOR`() {
      assertEquals(TokenType.OPERATOR, lex("==")[0].type)
    }

    @Test fun `asignacion es SYMBOL`() {
      assertEquals(TokenType.SYMBOL, lex("=")[0].type)
    }

    @Test fun `dos puntos es SYMBOL`() {
      assertEquals(TokenType.SYMBOL, lex(":")[0].type)
    }

    @Test fun `punto y coma es SYMBOL`() {
      assertEquals(TokenType.SYMBOL, lex(";")[0].type)
    }
  }

  // -------------------------------------------------------------------------
  // Texto del token — el texto original se preserva tal cual
  // -------------------------------------------------------------------------

  @Nested
  inner class TokenText {
    @Test fun `texto de keyword preservado`() {
      assertEquals("let", lex("let")[0].text)
    }

    @Test fun `texto de identificador preservado`() {
      assertEquals("myVar", lex("myVar")[0].text)
    }

    @Test fun `texto de numero preservado`() {
      assertEquals("123", lex("123")[0].text)
    }

    @Test fun `texto de string incluye comillas`() {
      assertEquals("\"hello\"", lex("\"hello\"")[0].text)
    }

    @Test fun `texto de operador preservado`() {
      assertEquals("+", lex("+")[0].text)
    }
  }

  // -------------------------------------------------------------------------
  // Posiciones — row y col son 0-indexed según el Lexer
  // -------------------------------------------------------------------------

  @Nested
  inner class Positions {
    @Test fun `primer token empieza en columna 0`() {
      val tokens = lex("let")
      assertEquals(Position(0, 0), tokens[0].pos)
    }

    @Test fun `fila es 0 para la primera linea`() {
      assertEquals(0, lex("let")[0].pos.row)
    }

    @Test fun `columna refleja el offset en la linea`() {
      // "let x" → 'x' empieza en col 4
      val tokens = lex("let x")
      assertEquals(4, tokens[1].pos.col)
    }

    @Test fun `columnas correctas en declaracion completa`() {
      // "let x : number ="
      //  0   4   6  8
      val tokens = lex("let x : number =")
      assertEquals(0, tokens[0].pos.col) // let
      assertEquals(4, tokens[1].pos.col) // x
      assertEquals(6, tokens[2].pos.col) // :
      assertEquals(8, tokens[3].pos.col) // number
      assertEquals(15, tokens[4].pos.col) // =
    }

    @Test fun `todos los tokens de la primera linea tienen row 0`() {
      val tokens = lex("let x : number = 42;")
      assertTrue(tokens.all { it.pos.row == 0 })
    }
  }

  // -------------------------------------------------------------------------
  // Declaraciones completas — secuencia de tipos en orden correcto
  // -------------------------------------------------------------------------

  @Nested
  inner class FullStatements {
    @Test fun `declaracion de numero produce 7 tokens`() {
      assertEquals(7, lex("let x: number = 42;").size)
    }

    @Test fun `declaracion de string produce 7 tokens`() {
      assertEquals(7, lex("let name: string = \"John\";").size)
    }

    @Test fun `orden de tipos en declaracion de numero`() {
      val types = lex("let x: number = 42;").map { it.type }
      assertEquals(
        listOf(
          TokenType.KEYWORD,
          TokenType.IDENTIFIER,
          TokenType.SYMBOL, // :
          TokenType.VARIABLE_TYPE,
          TokenType.SYMBOL, // =
          TokenType.NUMBER,
          TokenType.SYMBOL, // ;
        ),
        types,
      )
    }

    @Test fun `orden de tipos en declaracion de string`() {
      val types = lex("let name: string = \"John\";").map { it.type }
      assertEquals(
        listOf(
          TokenType.KEYWORD,
          TokenType.IDENTIFIER,
          TokenType.SYMBOL, // :
          TokenType.VARIABLE_TYPE,
          TokenType.SYMBOL, // =
          TokenType.STRING,
          TokenType.SYMBOL, // ;
        ),
        types,
      )
    }

    @Test fun `expresion aritmetica simple`() {
      val types = lex("a + b").map { it.type }
      assertEquals(
        listOf(TokenType.IDENTIFIER, TokenType.OPERATOR, TokenType.IDENTIFIER),
        types,
      )
    }

    @Test fun `expresion aritmetica compleja`() {
      val types = lex("a + b * c - d / e").map { it.type }
      assertEquals(
        listOf(
          TokenType.IDENTIFIER,
          TokenType.OPERATOR, // a +
          TokenType.IDENTIFIER,
          TokenType.OPERATOR, // b *
          TokenType.IDENTIFIER,
          TokenType.OPERATOR, // c -
          TokenType.IDENTIFIER,
          TokenType.OPERATOR, // d /
          TokenType.IDENTIFIER, // e
        ),
        types,
      )
    }
  }

  // -------------------------------------------------------------------------
  // Splitter — RegexSplitter de forma aislada
  // -------------------------------------------------------------------------

  @Nested
  inner class SplitterTests {
    private val splitter = RegexSplitter(TestTokenDefs)

    @Test fun `split devuelve coincidencias en orden`() {
      val matches = splitter.split("let x").map { it.text }.toList()
      assertEquals(listOf("let", "x"), matches)
    }

    @Test fun `split de linea vacia no produce coincidencias`() {
      assertEquals(0, splitter.split("").toList().size)
    }

    @Test fun `split captura indice correcto`() {
      val matches = splitter.split("a + b").toList()
      assertEquals(0, matches[0].index) // a
      assertEquals(2, matches[1].index) // +
      assertEquals(4, matches[2].index) // b
    }

    @Test fun `split ignora espacios entre tokens`() {
      val texts = splitter.split("let   x").map { it.text }.toList()
      assertEquals(listOf("let", "x"), texts)
    }
  }

  // -------------------------------------------------------------------------
  // Casos borde
  // -------------------------------------------------------------------------

  @Nested
  inner class EdgeCases {
    @Test fun `entrada vacia produce cero tokens`() {
      assertEquals(0, lex("").size)
    }

    @Test fun `espacios extra son ignorados`() {
      val tokens = lex("let   x")
      assertEquals(2, tokens.size)
      assertEquals(TokenType.KEYWORD, tokens[0].type)
      assertEquals(TokenType.IDENTIFIER, tokens[1].type)
    }

    @Test fun `let no es IDENTIFIER`() {
      assertEquals(TokenType.KEYWORD, lex("let")[0].type)
    }

    @Test fun `string y number son VARIABLE_TYPE, no IDENTIFIER`() {
      assertEquals(TokenType.VARIABLE_TYPE, lex("string")[0].type)
      assertEquals(TokenType.VARIABLE_TYPE, lex("number")[0].type)
    }

    @Test fun `string literal vacio`() {
      val tokens = lex("\"\"")
      assertEquals(1, tokens.size)
      assertEquals(TokenType.STRING, tokens[0].type)
      assertEquals("\"\"", tokens[0].text)
    }

    @Test fun `string literal con espacios`() {
      val tokens = lex("\"hello world\"")
      assertEquals(1, tokens.size)
      assertEquals(TokenType.STRING, tokens[0].type)
    }

    @Test fun `numero de multiples digitos`() {
      val tokens = lex("12345")
      assertEquals(1, tokens.size)
      assertEquals(TokenType.NUMBER, tokens[0].type)
      assertEquals("12345", tokens[0].text)
    }

    @Test fun `solo simbolos`() {
      val types = lex(": ; =").map { it.type }
      assertEquals(
        listOf(TokenType.SYMBOL, TokenType.SYMBOL, TokenType.SYMBOL),
        types,
      )
    }

    @Test fun `operador == no se confunde con asignacion =`() {
      val tokens = lex("==")
      assertEquals(1, tokens.size)
      assertEquals(TokenType.OPERATOR, tokens[0].type)
      assertEquals("==", tokens[0].text)
    }

    @Test fun `identificador con numeros al final`() {
      val tokens = lex("var123")
      assertEquals(1, tokens.size)
      assertEquals(TokenType.IDENTIFIER, tokens[0].type)
    }
  }
}
