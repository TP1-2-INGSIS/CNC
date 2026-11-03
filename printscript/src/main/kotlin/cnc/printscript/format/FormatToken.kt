package cnc.printscript.format

/** Categoría de un token "duro" (no whitespace) para el formateo preservador. */
enum class TokenKind {
    KEYWORD, // let, const, if, else
    IDENTIFIER,
    NUMBER,
    STRING,
    TYPE, // string, number, boolean (tras ':')
    OPERATOR, // + - * / ** ==
    SYMBOL, // : = ; ( ) { } ,
    WHITESPACE, // espacios/tabs/saltos (el "gap" preservable)
}

/** Segmento del source: un token duro o un gap de whitespace, con su texto literal. */
data class FormatToken(
    val kind: TokenKind,
    val text: String,
)

/** Tokeniza el source preservando el whitespace como segmentos propios (no lo descarta). */
object SourceTokenizer {
    private val keywords = setOf("let", "const", "if", "else")
    private val operators = listOf("**", "==", "+", "-", "*", "/")
    private val singleSymbols = setOf(':', '=', ';', '(', ')', '{', '}', ',')

    fun tokenize(source: String): List<FormatToken> {
        val tokens = mutableListOf<FormatToken>()
        var i = 0
        while (i < source.length) {
            val c = source[i]
            when {
                c.isWhitespace() -> {
                    val start = i
                    while (i < source.length && source[i].isWhitespace()) i++
                    tokens.add(FormatToken(TokenKind.WHITESPACE, source.substring(start, i)))
                }

                c == '"' -> {
                    val start = i
                    i++
                    while (i < source.length && source[i] != '"') i++
                    if (i < source.length) i++ // cerrar comilla
                    tokens.add(FormatToken(TokenKind.STRING, source.substring(start, i)))
                }

                c.isDigit() -> {
                    val start = i
                    while (i < source.length && (source[i].isDigit() || source[i] == '.')) i++
                    tokens.add(FormatToken(TokenKind.NUMBER, source.substring(start, i)))
                }

                c.isLetter() || c == '_' -> {
                    val start = i
                    while (i < source.length && (source[i].isLetterOrDigit() || source[i] == '_')) i++
                    val word = source.substring(start, i)
                    tokens.add(FormatToken(wordKind(word), word))
                }

                else -> {
                    val op = operators.firstOrNull { source.startsWith(it, i) }
                    if (op != null) {
                        tokens.add(FormatToken(TokenKind.OPERATOR, op))
                        i += op.length
                    } else if (c in singleSymbols) {
                        tokens.add(FormatToken(TokenKind.SYMBOL, c.toString()))
                        i++
                    } else {
                        tokens.add(FormatToken(TokenKind.SYMBOL, c.toString()))
                        i++
                    }
                }
            }
        }
        return tokens
    }

    private fun wordKind(word: String): TokenKind =
        when (word) {
            in keywords -> TokenKind.KEYWORD
            "string", "number", "boolean" -> TokenKind.TYPE
            else -> TokenKind.IDENTIFIER
        }
}
