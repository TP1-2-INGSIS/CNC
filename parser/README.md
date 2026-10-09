# Módulo: :parser

**Ruta:** `/parser`  
**Dependencias directas:** `:common`, `:token`, `:ast`  
**Consumidores:** `:app`  

---

## 1. Responsabilidad y Propósito (SRP)
- **Qué problema resuelve:** Transforma el flujo secuencial de tokens (`Sequence<Token>`) en un flujo estructurado de sentencias sintácticas (`Sequence<Result<Statement>>`). Combina un motor predictivo $LL(2)$ para sentencias con un algoritmo de precedencia de operadores (Pratt Parser) para expresiones aritméticas y compuestas.
- **Qué hace:**
  - Implementa el parser continuo con semántica *fail-fast* sobre `Cursor<Token>`.
  - Provee la interfaz extensible y covariante `StatementRule<out T : Statement>` con predicados de inicio predictivos $LL(2)$ `canStart(cursor)`.
  - Expone el builder fluido `StatementRule.builder<T>()` para construir reglas declarativas sin clases manuales.
  - Expone el flujo monádico `ParseFlow` (`cursor.flow()`) con operaciones de coincidencia tipo *railroad* (`expect`, `capture`, `optional`, `parseExpression`, `then`, `catch`) sin excepciones para control de flujo.
  - Provee las reglas estándar de PrintScript 1.0 y 1.1 (`StandardStatementRules.v1_0`, `StandardStatementRules.v1_1`).
  - Construye expresiones respetando precedencia y asociatividad mediante `ExpressionBuilder` (Pratt) retornando puros `Result<Expression>`.
- **Qué NO hace (Fronteras):**
  - No valida tipos estáticos ni existencia de variables en la tabla de símbolos (responsabilidad de `:semantic`).
  - No evalúa ni ejecuta código (responsabilidad de `:interpreter`).

---

## 2. Arquitectura y Componentes Clave

### Diagrama de Arquitectura
```mermaid
flowchart TD
    Toks["Tokens (Cursor Token)"] --> Parser["Parser.parse()"]
    Parser --> MatchRule{"rules.firstOrNull { canStart }"}
    MatchRule -- Coincide --> Flow["cursor.flow()"]
    Flow --> Primitives[".expect() / .capture() / .optional()"]
    Primitives --> Pratt["ExpressionBuilder.parse() (Pratt)"]
    Pratt --> Expr["Result Success(Expression)"]
    Flow --> Then[".then { Statement }"]
    Then --> Stmt["Result Success(Statement)"]
    MatchRule -- No coincide --> SynErr["Failure(Syntax error, PARSER)"]
    Stmt --> Res["yield Success(Statement)"]
    SynErr --> YieldFail["yield Failure & break"]
```

### Entidades de Dominio e Interfaces
1. **`Parser`:**
   - Orquestador del análisis sintáctico: `fun parse(tokens: Sequence<Token>): Sequence<Result<Statement>>`.
   - Si una sentencia falla sintácticamente, emite `Failure` y finaliza la secuencia limpiamente (*fail-fast*).
2. **`StatementRule<out T : Statement>`:**
   - Contrato declarativo de regla de sentencia:
     - `val tag: String`: Identificador de la regla.
     - `fun canStart(cursor: Cursor<Token>): Boolean`: Predicado $LL(2)$ que inspecciona tokens con `cursor.matches(...)` sin consumirlos.
     - `fun parse(cursor, expressionParser, nextStatement): Result<T>`.
3. **`StatementRuleBuilder<T : Statement>`:**
   - API fluida para declarar reglas sin sobrecarga de clases:
     `StatementRule.builder<Declaration>().withName("let").starts { ... }.parse { cursor, exprParser -> ... }.build()`
4. **`ParseFlow` (`OkFlow` / `FailedFlow`):**
   - Máquina de estados monádica sobre `Cursor<Token>`:
     - `expect(def / type, text)`: Valida y consume el token esperado; ante error transiciona a `FailedFlow`.
     - `capture(type, text) { ... }`: Captura el valor del token.
     - `optional(def / type) { ... }`: Consume si coincide, sin fallar si no.
     - `parseExpression(expressionParser) { ... }`: Ejecuta el Pratt parser funcionalmente.
     - `optionalExpression(trigger, expressionParser) { ... }`: Maneja expresiones opcionales (ej. asignación inicial).
     - `then { ... }`: Produce `Success(Statement)` si todo el flujo fue exitoso.
5. **`ExpressionBuilder`:**
   - Motor Pratt funcional: configura recetas para hojas, operadores con precedencia y asociatividad, y expresiones de llamada (`CallExpression`) configurables, retornando `Result<Expression>` puro.

---

## 3. Manejo de Errores y Pipeline Funcional
- **ErrorType asociado:** `ErrorType.PARSER`.
- **Garantías:** Cero excepciones para control de flujo. Los fallos sintácticos transicionan monádicamente a `FailedFlow` y retornan directamente como `Failure(msg, ErrorType.PARSER)` preservando la posición exacta de fila y columna.

---

## 4. Ejemplo Práctico Autónomo (End-to-End)

```kotlin
package example

import cnc.ast.*
import cnc.common.Success
import cnc.common.asCharCursor
import cnc.lexer.Lexer
import cnc.lexer.rules.StandardRules
import cnc.parser.Parser
import cnc.parser.expression.Associativity
import cnc.parser.expression.ExpressionBuilder
import cnc.parser.expression.OperatorDef
import cnc.parser.rule.StandardStatementRules
import cnc.token.Token
import cnc.token.TokenType

fun main() {
    // 1. Configurar ExpressionBuilder (Pratt)
    val exprParser = ExpressionBuilder(
        recipes = mapOf(
            "num" to { token: Token -> NumberLiteral(token.text.toDouble()) },
            "id" to { token: Token -> Identifier(token.text) }
        ),
        operators = listOf(
            OperatorDef("+", precedence = 1),
            OperatorDef("*", precedence = 2),
            OperatorDef("**", precedence = 3, associativity = Associativity.RIGHT)
        )
    )

    // 2. Configurar Parser con reglas estándar
    val parser = Parser(
        rules = StandardStatementRules.printScript10,
        expressionParser = exprParser
    )

    // 3. Parsear tokens simulados
    val lexer = Lexer(
        StandardRules.whitespace(),
        StandardRules.integerNumber(),
        StandardRules.standardIdentifier(
            keywords = mapOf("let" to TokenType.KEYWORD, "number" to TokenType.VARIABLE_TYPE)
        ),
        StandardRules.symbols(mapOf(":" to TokenType.SYMBOL, "=" to TokenType.SYMBOL, ";" to TokenType.SYMBOL, "+" to TokenType.OPERATOR, "*" to TokenType.OPERATOR))
    )

    val tokens = lexer.tokenize("let x: number = 10 + 2 * 3;".asCharCursor())
    val statements = parser.parse(tokens)

    for (result in statements) {
        when (result) {
            is Success -> {
                val stmt = result.data
                println("Sentencia parseada con éxito: $stmt")
                // Declaration(name=x, type=number, value=BinaryExpression(...), isMutable=true)
            }
            is cnc.common.Failure -> {
                println("Error de sintaxis: [${result.type}] ${result.msg}")
            }
        }
    }
}
```

---

## 5. Integración en el Pipeline del Compilador
- **Entrada:** `Sequence<Token>` provista por el `:lexer`.
- **Salida:** `Sequence<Result<Statement>>` consumida por el analizador de tipos `:semantic`.

---

## 6. Decisiones Técnicas y Tradeoffs
- **Predicción $LL(2)$ Determinista:** La inspección de a lo sumo dos tokens (`canStart(peek 0, peek 1)`) elimina el backtracking costoso y permite saber de inmediato qué regla ejecutar (ej: `let` $\rightarrow$ Declaración, `id` seguido de `=` $\rightarrow$ Asignación, `id` seguido de `(` $\rightarrow$ Llamada).
- **Pratt Parser para Expresiones:** Separa limpiamente la estructura de alto nivel de las sentencias del cálculo de precedencias y asociatividad matemática de las expresiones.
