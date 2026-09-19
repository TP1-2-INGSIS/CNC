# 🚀 PrintScript 1.1: Technical Changelog

Este documento detalla todas las modificaciones arquitectónicas y de código implementadas en el motor **CNC** para soportar la especificación de PrintScript 1.1. 

Cada sección explica el **por qué** del cambio y muestra fragmentos de **cómo** se resolvió a nivel código.

---

## 1. Soporte para Funciones como Expresiones (`CallExpression`)

> [!IMPORTANT]
> **Motivo:** En PrintScript 1.0, las funciones solo se podían invocar como sentencias sueltas (`println(x);`). Para la v1.1, funciones como `readInput()` deben poder **retornar valores** y ser asignadas a variables (`let x = readInput();`).

### Implementación
**1. Parser:** Se modificó `ExpressionBuilder.kt` (nuestro Pratt Parser) agregando *lookahead* para predecir si un identificador es en realidad la llamada a una función.
```kotlin
// Antes: Solo detectaba identificadores simples.
// Ahora: Si al identificador le sigue un '(', es un CallExpression.
val next = cursor.peek()
if (next != null && next.text == "(") {
    cursor.advance() // Consume '('
    val args = parseArguments(cursor)
    return CallExpression(token.text, args)
}
```

**2. Semántica e Intérprete:** Se registró el tipo de retorno (`string`) en las reglas semánticas y se inyectaron las funciones del sistema (`builtins`) directamente en el evaluador de expresiones.
```kotlin
// StandardExpressionTypeRules.kt
if (expr.function == "readInput" || expr.function == "readEnv") {
    Success("ok", "string")
}
```

---

## 2. Condicionales y Bloques (`IfStatement` & `BlockStatement`)

> [!NOTE]
> **Motivo:** Introducir control de flujo. Se requería que el motor pudiera aislar variables dentro de llaves `{ }` (Scope) y decidir qué bloque ejecutar en base a una condición booleana.

### Implementación
**1. Parser Recursivo:** Se inyectó una función `action` al contexto del parser para permitir que una regla llame a la lectura de otras sentencias de forma recursiva.
```kotlin
// StatementRule.kt - Creación de parseBlock()
override fun parseBlock(): BlockStatement {
    expect(TokenType.SYMBOL, "{")
    val stmts = mutableListOf<Statement>()
    while (peek()?.text != "}") {
        stmts.add(parseStatement()) // < Llamada recursiva
    }
    expect(TokenType.SYMBOL, "}")
    return BlockStatement(stmts)
}
```

**2. Evaluador de Scope:** El `BlockEvaluator` ahora genera entornos "hijos" para encapsular las variables locales.
```kotlin
// StatementEvaluators.kt
class BlockEvaluator : StatementEvaluator<BlockStatement> {
    override fun evaluate(statement: BlockStatement, environment: Environment, ...): Result<Unit> {
        val blockEnv = environment.createChild() // < Aislamiento de variables
        for (stmt in statement.statements) {
            interpreter.interpret(stmt, blockEnv)
        }
    }
}
```

---

## 3. Tipos BooleanosNativos (`true` / `false`)

> [!WARNING]
> **Motivo:** Aunque la estructura del `if` estaba, el lenguaje no reconocía las palabras clave `true` o `false`, haciendo imposible escribir condiciones directas o banderas condicionales.

### Implementación
Se habilitaron las palabras clave a lo largo de **toda la cadena de procesamiento**:
1. **Lexer:** Alta de tokens en `Tokens.kt` (`true`, `false`, `boolean`).
2. **Parser:** Mapeo de tokens a AST en `ParserConfig.kt`.
```kotlin
// ParserConfig.kt
recipes = mapOf(
    SymbolTokenDef("true", "true") to { _ -> BooleanLiteral(true) },
    SymbolTokenDef("false", "false") to { _ -> BooleanLiteral(false) }
    // ...
)
```
3. **Semántica:** Alta del tipo `"boolean"` en la tabla de símbolos válida de `SemanticConfig.kt`.

---

## 4. Fix Léxico para Números Decimales

> [!CAUTION]
> **Motivo:** El Lexer estaba configurado solo para enteros (`integerNumber`). Si leíamos `3.14`, el sistema explotaba porque lo dividía en tres tokens: `3`, `.` (símbolo inválido) y `14`.

### Implementación
Se creó una nueva regla de reconocimiento basada en expresiones regulares ligeras que soporta el punto decimal de forma contigua.
```kotlin
// StandardRules.kt
fun decimalNumber(tokenType: TokenType): LexerRule = PatternRule(
    startPredicate = Char::isDigit,
    // Permite dígitos continuos o un único punto decimal
    continuePredicate = { it.isDigit() || it == '.' },
    tokenType = tokenType
)
```

---

## 5. Validación Temprana de Constantes (`const`)

> [!IMPORTANT]
> **Motivo:** Las constantes inmutables no pueden nacer vacías (`const pi: number;`), pero el parser de v1.0 lo permitía. 

### Implementación
Se agregó una protección *fail-fast* (falla rápida) directo en la regla de declaración del Parser.
```kotlin
// StandardStatementRules.kt - declarationRule
if (!isMutable && initializer == null) {
    throw ParseAbortException(
        Failure("Syntax error: const declaration '${name}' must have an initializer")
    )
}
```

---

## 6. Extensión de Herramientas Periféricas (Formatter & Linter)

> [!NOTE]
> **Motivo:** Al agregar sentencias de Bloque, ni el Formatter ni el Linter sabían qué hacer con el código dentro de un `{ }`, ya que sus algoritmos originales eran completamente planos y no-recursivos.

### Implementación

**1. Formatter:** Se crearon las reglas de conversión a *String* para que tabulen correctamente el código interno.
```kotlin
// Formatter.kt (Config)
val stmts = block.statements.mapNotNull { inner ->
    printScriptStatementRules.firstNotNullOfOrNull { it.tryFormat(inner, ctx) }
}
// Añade 4 espacios de indentación a todo el bloque interno
"{\n" + stmts.joinToString("\n") { "    " + it.replace("\n", "\n    ") } + "\n}"
```

**2. Linter:** Se reestructuró el motor central `CNCLinter.kt` para que recorra el AST mediante recursión en profundidad.
```kotlin
// CNCLinter.kt
private fun checkRecursively(statement: Statement): List<String> {
    // ... evalúa el nodo actual ...
    when (statement) {
        is BlockStatement -> {
            for (stmt in statement.statements) warnings.addAll(checkRecursively(stmt))
        }
        is IfStatement -> {
            warnings.addAll(checkRecursively(statement.thenBlock))
            statement.elseBlock?.let { warnings.addAll(checkRecursively(it)) }
        }
    }
}
```
