# Resoluciones de Impedimentos PrintScript 1.1

Este documento detalla todas las modificaciones funcionales realizadas al motor **CNC** para dar soporte a las funcionalidades críticas de la versión 1.1 de PrintScript, resolviendo los bloqueos definidos en `analisis-printscript-1.1.md`.

## Puntos Resueltos

### Punto 1: `CallExpression` (Soporte para retornar valores)
- **Problema:** Las funciones solo podían llamarse como sentencias (`Call : Statement`), impidiendo usarlas en asignaciones (ej: `let x = readInput();`).
- **Solución:** 
  - Se confirmó la existencia de `CallExpression : Expression` en el AST.
  - Se implementó *lookahead* en el `ExpressionBuilder` (Parser Pratt) para matchear un `Identifier` seguido de `(` y parsear los argumentos.
  - Se agregó la regla semántica en `StandardExpressionTypeRules` para validar tipos y garantizar que devuelven `string`.
  - Se inyectaron los métodos nativos (`builtins`) al `ExpressionEvaluator` del intérprete para ejecutar estas llamadas sin romper la arquitectura.

### Punto 2 y 9: Nodos `IfStatement` y `BlockStatement`
- **Problema:** Faltaba soporte completo para bloques y sentencias condicionales (`if`/`else`).
- **Solución:**
  - **Parser (Punto 9):** Se extendió la interfaz `ParseContext` y el motor `Parser.kt` para inyectar recursividad (`nextStatement`). Esto permitió definir métodos de lectura como `parseBlock()`.
  - Se creó y registró la `ifRule` capaz de parsear recursivamente la condición y los bloques de código.
  - **Intérprete:** Se añadieron `BlockEvaluator` (que encapsula variables en un *scope local*) e `IfEvaluator` (que bifurca la ejecución al bloque correspondiente), registrándolos en `InterpreterPresets.kt`.

### Punto 7: Integración del tipo `boolean` (Crítico)
- **Problema:** Pese a la estructura del `if`, el motor no reconocía `true` o `false`.
- **Solución:**
  - Se definieron los tokens faltantes (`true`, `false`, `if`, `else`, `const`, `boolean`) en el repositorio central de tokens (`Tokens.kt`).
  - Se añadieron *recipes* en el `ParserConfig.kt` para transformar esos tokens en el nodo `BooleanLiteral`.
  - Se autorizó `"boolean"` como tipo nativo del lenguaje en `SemanticConfig.kt` y se agregaron las reglas semánticas y de ejecución en el intérprete.

### Punto 8: Soporte para números decimales (Crítico)
- **Problema:** Números como `3.14` causaban pánico en el Lexer rompiendo el flujo.
- **Solución:**
  - Se implementó una nueva regla léxica `decimalNumber` en `StandardRules.kt` capaz de aceptar de cero a un punto flotante de forma contigua.
  - Se modificó `LexerConfig.kt` para reemplazar el lector primitivo de enteros por este nuevo lector decimal.

### Punto 6: Validación de variables `const` 
- **Problema:** El parser permitía código como `const pi: number;`, lo cual es ilegal en PrintScript 1.1.
- **Solución:**
  - Se aplicó una validación temprana del tipo "fail-fast" en `declarationRule` (`StandardStatementRules.kt`) para verificar que las variables inmutables contengan de manera obligatoria una expresión inicializadora.

---
## Siguientes Pasos (Completados)
- **Punto 3:** Refactor del diseño de `ExpressionEvaluator` para que respete Open/Closed (desacoplamiento de `unaryOperations: Map<String, UnaryOperation>` y centralización en `NumberOperations`).
- **Punto 10 y 11:** Expansión del Formatter (recursión en `FormatContext` para bloques), Linter y soporte de versiones en el CLI (`ConfigFactory` y comandos `run`/`validate`).
- **Reporte completo:** Véase `docs/reports/2026-09-15_printscript-1.1-resolutions-refactor.md`.

