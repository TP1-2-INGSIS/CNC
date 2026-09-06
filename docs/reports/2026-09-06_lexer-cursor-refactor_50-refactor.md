# Reporte: Refactorización del Lexer y Pipeline de Streaming (100% Lazy)

**Fecha:** 2026-09-06  
**Rama:** `50-refactor`  
**Tema:** Desacoplamiento del Lexer, Cursor genérico, LineIndex incremental y streaming en una sola pasada.

---

## 1. Contexto y Diagnóstico del Problema

El diseño previo del `Lexer` presentaba varios problemas de acoplamiento y rendimiento:
1. **Acoplamiento I/O en el Lexer:** `Lexer` recibía directamente `ContentManager` y dependía de `CharStream(Reader)`, atando el análisis léxico a la lectura de archivos.
2. **Alocación masiva en heap:** `CharStream` actualizaba `Position(row, col)` por cada carácter individual mediante `Position.advance(char)`, creando cientos de miles de objetos en memoria. Además, usaba `mutableListOf.removeFirst()`, provocando un desplazamiento de memoria de orden $O(N)$ por carácter.
3. **Contaminación con tokens dummy:** Para poder rastrear saltos de línea acumulando sobre tokens, las reglas tenían que generar tokens de espacios en blanco (`RawToken(WHITESPACE, "...")`), para luego ser filtrados por el pipeline.
4. **Falta de genericidad:** No existía una abstracción de cursor reutilizable para etapas posteriores (como el parser).

---

## 2. Decisiones de Diseño y Arquitectura

### A. Abstracción `Cursor<T>`
En lugar del nombre previo `LookaheadStream`, se adoptó **`Cursor<T>`**:
* Mantiene `currentOffset: Int`, que contabiliza los elementos consumidos en $O(1)$.
* `peek(offset: Int)` seguro: si el offset es negativo o supera los elementos disponibles, retorna `null` sin lanzar excepciones de runtime.
* Buffer basado en `ArrayDeque<T>` para el lookahead, con consumo constante $O(1)$.

### B. `LineIndex` Incremental y $O(\log L)$ Binary Search
* Mapea 1D offsets a 2D `Position(row, col)`.
* Almacena únicamente los enteros correspondientes a los offsets donde comienza cada línea (`lineStarts: List<Int>`).
* Emplea el algoritmo built-in `binarySearch` de la biblioteca estándar de Kotlin para resolver la fila y calcular la columna:
  $$\text{col} = \text{offset} - \text{lineStarts}[\text{row}]$$

### C. Streaming 100% Lazy en 1 Sola Pasada (`ContentManager.openStream`)
* Constante macro: `DEFAULT_STREAM_BUFFER_SIZE = 8192` (8 KB), alineada con los bloques del filesystem y la caché L1 de la CPU.
* El `Reader` se lee una única vez en bloques de 8 KB. Conforme los caracteres fluyen hacia `Cursor<Char>`, se registran los `\n` en el `LineIndex`.
* **Memoria de texto:** $O(1)$ constante (8 KB). Un archivo de 100 GB se procesa sin desbordar la memoria.

### D. `Lexer` Puro y Reglas Desacopladas
* El `Lexer` solo expone `tokenize(cursor: Cursor<Char>): Sequence<RawToken>`.
* Cero dependencias de `Position`, `LineIndex` o `Token`.
* Las reglas (`LexerRule`) devuelven `RuleResult.Matched(type, text)` o `RuleResult.Skipped`. Cero lógica de offsets en las reglas.
* `WhitespaceRule` simplemente consume espacios y devuelve `RuleResult.Skipped`, sin emitir tokens dummy.

### E. Enriquecimiento Geométrico en el Compilador
* `Compiler.compile(content: ContentManager)` orquesta el pipeline:
  $$\text{ContentManager.openStream()} \to \text{Lexer.tokenize(cursor)} \to \text{rawTokens.withPositions(lineIndex)} \to \text{Parser.getASTs(tokens)}$$

---

## 3. Estado de la Suite de Pruebas

Todas las suites de prueba ejecutadas arrojaron **100% PASS**:
* `:common:test` (Cursor, LineIndex, ContentManager, Result, Position)
* `:token:test` (RawToken, withPositions, Token definitions)
* `:lexer:test` (RawTokenization, TokenTypes, TokenText, Positions, FullStatements, EdgeCases, Trie)
* `:parser:test` (Grammars, Steps, AST generation)
* `:semantic:test` (Type checks, validations)
