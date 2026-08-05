package org

import org.lexer.create_lexer

class App {
    val greeting: String
        get() {
            return "Hello World!"
        }
}

fun main() {
    create_lexer();
    println(App().greeting);
}
