package org.lexer

import org.utils.Position
import org.config.Token
import org.config.TokenIdentifier
import org.config.TokenType

// necesito el content porque asi puedo saber donde quede la ultima vez
// que saque un token.
//
// Lo hice object porque es indiferente tener una clase si solo vamos a
// tener un lexer en todo el compiler. De paso nos ahorramos de hacer
// un Lexer() y tener que ponerle esos parentesis feos.
//
// TODO: Deberiamos hacer una interfaz? interface Lexer y hacer una impl?
// yo creo que no, pero la dejo picando

interface Lexer {
  fun getTokens(content: ContentManager): Sequence<Token>
}

// ---------------------------------------------------------------------------
// CharLexer: (sin regex)
// ---------------------------------------------------------------------------

class CharLexer : Lexer {
  private var position: Int = 0
  private var line: Int = 1
  private var column: Int = 1
  private var input: String = ""

  private val currentChar: Char
    get() = if (position >= input.length) '\u0000' else input[position]

  private fun peekChar(): Char {
    val nextPosition = position + 1
    return if (nextPosition >= input.length) '\u0000' else input[nextPosition]
  }

  private fun advance() {
    if (currentChar == '\n') {
      line++
      column = 1
    } else {
      column++
    }
    position++
  }

  override fun getTokens(content: ContentManager): Sequence<Token> {
    // Unir todas las líneas en un único string para procesar carácter a carácter
    this.input = content.getLines().joinToString("\n")
    this.position = 0
    this.line = 1
    this.column = 1

    return sequence {
      while (position <= input.length) {
        skipWhitespace()

        val startLine = line
        val startColumn = column

        if (currentChar == '\u0000') break

        val token = when (currentChar) {
          '=' -> {
            if (peekChar() == '=') {
              advance()
              Token(TokenType.EQUAL, Position(startLine, startColumn), "==")
            } else {
              Token(TokenType.ASSIGN, Position(startLine, startColumn), "=")
            }
          }
          '+', '-', '*', '/' -> Token(TokenType.OPERATOR, Position(startLine, startColumn), currentChar.toString())
          ';' -> Token(TokenType.SEMICOLON, Position(startLine, startColumn), ";")
          ':' -> Token(TokenType.COLON, Position(startLine, startColumn), ":")
          '"' -> {
            val literal = readString()
            yield(Token(TokenType.STRING, Position(startLine, startColumn), literal))
            continue
          }
          else -> {
            if (currentChar.isLetter() || currentChar == '_') {
              val literal = readIdentifier()
              val type = TokenIdentifier.type(literal)
              yield(Token(type, Position(startLine, startColumn), literal))
              continue
            } else if (currentChar.isDigit()) {
              val literal = readNumber()
              yield(Token(TokenType.LITERAL, Position(startLine, startColumn), literal))
              continue
            } else {
              Token(TokenType.INVALID, Position(startLine, startColumn), currentChar.toString())
            }
          }
        }

        advance()
        yield(token)
      }
    }
  }

  private fun readIdentifier(): String {
    val start = position
    while (currentChar.isLetterOrDigit() || currentChar == '_') {
      advance()
    }
    return input.substring(start, position)
  }

  private fun readNumber(): String {
    val start = position
    while (currentChar.isDigit()) {
      advance()
    }
    return input.substring(start, position)
  }

  private fun readString(): String {
    val start = position
    advance() // consumir la comilla inicial
    while (currentChar != '"' && currentChar != '\u0000') {
      advance()
    }
    advance() // consumir la comilla final
    return input.substring(start, position)
  }

  private fun skipWhitespace() {
    while (currentChar == ' ' || currentChar == '\t' || currentChar == '\n' || currentChar == '\r') {
      advance()
    }
  }
}

// ---------------------------------------------------------------------------
// RegexLexer: implementación original basada en RegexSplitter
// ---------------------------------------------------------------------------

class RegexLexer : Lexer {
  private val splitter: Splitter = RegexSplitter()

  override fun getTokens(content: ContentManager): Sequence<Token> {
    return content
      .getLines()
      .withIndex()
      .flatMap { (index, line) ->
        splitter.split(line).map { match ->
          Token(
            TokenIdentifier.type(match.value),
            Position(index + 1, match.range.first + 1),
            match.value
          )
        }
      }
  }
}
