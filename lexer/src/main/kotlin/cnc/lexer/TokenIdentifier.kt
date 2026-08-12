package org.lexer

import org.config.TokenType
import org.config.TokenDefinition
import org.config.TokenDefinitionProvider

object TokenIdentifier {

    fun type(value: String): TokenType {
        for (type in TokenDefinitionProvider.getTypes()) {
            val definitions = TokenDefinitionProvider.getDefinitions(type)!!
            
            for (def in definitions) {
                if (!def.match(value)) continue
                return type 
            }
        }

        return TokenType.INVALID
    }
}
