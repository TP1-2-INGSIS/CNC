package cnc.parser.cursor

import cnc.common.Cursor
import cnc.parser.flow.OkFlow
import cnc.parser.flow.ParseFlow
import cnc.token.Token
import cnc.token.TokenDefinition
import cnc.token.TokenType

/**
 * Initiates a monadic [ParseFlow] on top of this [Cursor] of [Token]s.
 */
fun Cursor<Token>.flow(): ParseFlow = OkFlow(this)

/**
 * Returns true if the next token matches the given [TokenDefinition].
 */
fun Cursor<Token>.matches(def: TokenDefinition): Boolean {
    val first = peek(0) ?: return false
    return def.match(first.text)
}

/**
 * Returns true if the next token matches the given [TokenType].
 */
fun Cursor<Token>.matches(type: TokenType): Boolean {
    val first = peek(0) ?: return false
    return first.type == type
}

/**
 * Returns true if the next token matches any of the given [TokenDefinition]s.
 */
fun Cursor<Token>.matches(defs: Collection<TokenDefinition>): Boolean {
    val first = peek(0) ?: return false
    return defs.any { it.match(first.text) }
}

/**
 * LL(2) inspection: returns true if the next token has [type] and matches any of [defs].
 */
fun Cursor<Token>.matches(type: TokenType, defs: Collection<TokenDefinition>): Boolean {
    val first = peek(0) ?: return false
    return first.type == type && defs.any { it.match(first.text) }
}

/**
 * LL(2) inspection: returns true if token at offset 0 matches [type] and token at offset 1 matches [def].
 */
fun Cursor<Token>.matches(type: TokenType, def: TokenDefinition): Boolean {
    val first = peek(0) ?: return false
    if (first.type != type) return false
    val second = peek(1) ?: return false
    return def.match(second.text)
}

/**
 * LL(2) inspection: returns true if token at offset 0 matches [firstDef] and token at offset 1 matches [secondDef].
 */
fun Cursor<Token>.matches(firstDef: TokenDefinition, secondDef: TokenDefinition): Boolean {
    val first = peek(0) ?: return false
    if (!firstDef.match(first.text)) return false
    val second = peek(1) ?: return false
    return secondDef.match(second.text)
}
