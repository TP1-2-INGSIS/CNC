package cnc.token

import cnc.common.LineIndex
import cnc.common.Position
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class RawTokenTest {

    @Test
    fun `withPositions maps raw tokens into tokens with positions`() {
        val lineIndex = LineIndex.build("let a = 1;\nlet b = 2;")
        val rawTokens = sequenceOf(
            RawToken(TokenType.KEYWORD, "let", 0),
            RawToken(TokenType.IDENTIFIER, "a", 4),
            RawToken(TokenType.KEYWORD, "let", 11),
            RawToken(TokenType.IDENTIFIER, "b", 15)
        )

        val tokens = rawTokens.withPositions(lineIndex).toList()

        assertEquals(4, tokens.size)
        assertEquals(Token(TokenType.KEYWORD, Position(0, 0), "let"), tokens[0])
        assertEquals(Token(TokenType.IDENTIFIER, Position(0, 4), "a"), tokens[1])
        assertEquals(Token(TokenType.KEYWORD, Position(1, 0), "let"), tokens[2])
        assertEquals(Token(TokenType.IDENTIFIER, Position(1, 4), "b"), tokens[3])
    }
}
