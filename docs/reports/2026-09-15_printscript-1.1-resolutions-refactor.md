# Reporte Técnico y de Arquitectura: Resolución de Bloqueantes y Refactor de Diseño en PrintScript 1.1

**Fecha:** 2026-09-15  
**Tópico:** `printscript-1.1-resolutions-refactor`  
**Rama:** `printscript-1.1`  
**Ubicación:** `docs/reports/2026-09-15_printscript-1.1-resolutions-refactor.md`  

---

## 1. Resumen Ejecutivo y Propósito

El presente documento registra la auditoría integral, resolución de bloqueantes funcionales y refactorizaciones de diseño arquitectónico realizadas sobre el motor **CNC** tras la incorporación de las especificaciones de **PrintScript 1.1** (commit `9482e2f`).

El objetivo principal consistió en alinear todo el compilador y runtime con la filosofía de diseño del proyecto:
1. **Tipado estático estricto y predecible:** Erradicación de casteo inseguro, dynamic maps no tipados (`<*>`), o strings mágicos hardcodeados.
2. **Manejo funcional de errores:** Propagación de fallos a través de `Result.Failure` sin arrojar excepciones no controladas a la JVM (`error(...)` o `ClassCastException`).
3. **Principios SOLID (SRP, OCP, DIP):** Desacoplamiento de operadores y builtins vía inyección de dependencias, extensibilidad mediante Builders fluidos y aislamiento de versiones.
4. **Ámbitos léxicos jerárquicos:** Modelado formal de scopes en análisis semántico y runtime para bloques condicionales e inmutabilidad estricta de constantes.

---

## 2. Diagnóstico de Bloqueantes Previos vs. Soluciones Implementadas

### 2.1. Análisis Semántico (`:semantic`)

* **Problema 1: Ámbito Plano en `SymbolTable`:**  
  La tabla de símbolos solo poseía un único mapa plano `mutableMapOf<String, String>()`. Al ejecutar un bloque condicional `if (cond) { let x: number = 1; } else { let x: number = 2; }`, el compilador arrojaba error semántico indicando que `x` ya había sido declarada. Además, las variables declaradas dentro de un bloque fugaban al ámbito global o impedían el *shadowing* válido.
  * **Solución:** Se transformó `SymbolTable` en una estructura jerárquica con referencia a un ámbito padre (`parent: SymbolTable?`) y un método factory `createChild()`. Se definió la entidad `Symbol(type: String, isMutable: Boolean)`. Ahora `checkBlock()` crea un ámbito hijo que encapsula las declaraciones locales y permite *shadowing* legítimo sin fugas.

* **Problema 2: Ausencia de Validación de Condición Booleana:**  
  `SemanticAnalyzer.checkIf()` no validaba el tipo de la expresión condicional, permitiendo sintácticamente expresiones como `if ("hola") { ... }`.
  * **Solución:** Se añadió validación estricta exigiendo que `expressionResolver.resolveType(condition) == "boolean"`. De lo contrario, se emite un `Failure("Condición del 'if' debe ser de tipo boolean...", ErrorType.SEMANTIC)`.

* **Problema 3: Mutabilidad de `const` no Verificada:**  
  Aunque el parser reconocía la palabra clave `const`, el analizador semántico permitía reasignar valores a variables constantes (`checkAssignment` no consultaba mutabilidad).
  * **Solución:** `Symbol` almacena la bandera `isMutable`. `checkAssignment()` verifica `context.isMutable(target)`. Si es falsa, retorna un `Failure("No se puede reasignar la constante...", ErrorType.SEMANTIC)`.

---

### 2.2. Intérprete y Builtins (`:interpreter`)

* **Problema 1: Fallo Crítico en Runtime por Falta de `readInput` y `readEnv`:**  
  La especificación 1.1 requiere funciones nativas para leer del usuario y del entorno. Al no estar registradas en los presets del intérprete, cualquier script válido en 1.1 fallaba en ejecución con `Function 'readInput' not found`.
  * **Solución:** Se creó el preset `InterpreterPresets.v1_1(input, envProvider, output)` que registra `readInput` y `readEnv` como `BuiltinMethod`s de primera clase, delegando en abstracciones funcionales inyectables.

* **Problema 2: Operaciones Unarias Hardcodeadas y Asimétricas:**  
  Mientras que las operaciones binarias (`+`, `-`, `*`, `/`) estaban prolijamente desacopladas mediante `BinaryOperation` y `StandardBinaryOperations`, las expresiones unarias (`-` y `+`) estaban "cableadas" directamente adentro de un bloque `when` gigante dentro de `ExpressionEvaluators.kt`.
  * **Solución:** Se formalizó la interfaz funcional `UnaryOperation`, se creó el objeto `StandardUnaryOperations`, y se centralizó toda la aritmética en `NumberOperations` (`negate`, `positive`, `add`, `subtract`, `multiply`, `divide`). `ExpressionEvaluator` ahora recibe un mapa inyectado `unaryOperations: Map<String, UnaryOperation>`, respetando el Principio de Abierto/Cerrado (OCP).

---

### 2.3. Parser y Representación de Tokens (`:token`, `:parser`)

* **Problema 1: Excepciones de JVM no Controladas en `ExpressionBuilder`:**  
  En casos de error sintáctico en expresiones, `ExpressionBuilder.kt` invocaba la función estándar de Kotlin `error(...)`, lanzando un `IllegalStateException` descontrolado y abortando abruptamente el proceso.
  * **Solución:** Se sustituyeron todas las llamadas a `error(...)` por `ParseAbortException(Failure(..., ErrorType.PARSER))`, garantizando que todos los errores sintácticos se atrapen y retornen de forma limpia como `Result.Failure`.

* **Problema 2: Strings Mágicos Hardcodeados en las Reglas:**  
  En `StandardStatementRules.kt`, las reglas realizaban comprobaciones directas contra cadenas literales: `first.text in setOf("let", "const")`, `kw.text == "let"`, `expect(TokenType.SYMBOL, ":")`, `match(TokenType.SYMBOL, "=")`, etc. Esto contradecía la existencia del modelo canónico `TokenDefinition` / `SymbolTokenDef`.
  * **Solución:** 
    1. Se trasladaron las definiciones canónicas `CncKeywords`, `CncSymbols` y `CncPatterns` al módulo `:token` ([`CncTokens.kt`](file:///c:/Users/bauti/projects/CNC/token/src/main/kotlin/cnc/token/CncTokens.kt)), haciéndolas accesibles para todo el compilador.
    2. Se extendió `ParseContext` con `match(def: TokenDefinition)` y `expect(def: TokenDefinition)`.
    3. Todas las comprobaciones de reglas ahora utilizan directamente las definiciones canónicas de tokens (`CncKeywords.LET`, `CncSymbols.COLON`, etc.).

* **Problema 3: Código Disperso y Duplicado en `declarationRuleV10`:**  
  Existían variables sueltas a nivel de archivo (`val declarationRule`, `val declarationRuleV10`) con 30 líneas de código duplicado solo para variar si se admitía `const` o solo `let`.
  * **Solución:** Se implementaron dos Builders cohesivos:
    - `StatementRule.builder<T>(tag)`: Para construir reglas individuales de forma declarativa con `canStartWith(...)` y `parse { ... }`.
    - `StatementRulesBuilder`: Para ensamblar conjuntos de reglas versión por versión (`withDeclaration(...)`, `withAssignment()`, `withCall()`, `withIf()`).
    - La regla de declaración se factorizó en `createDeclarationRule(vararg keywords: TokenDefinition)`, reutilizando la misma lógica para v1.0 (`CncKeywords.LET`) y v1.1 (`CncKeywords.LET, CncKeywords.CONST`).

---

### 2.4. Formateador y CLI (`:formatter`, `:app`)

* **Problema 1: Acoplamiento Global en el Formateador de Bloques:**  
  Las reglas de formateo para bloques (`blockStatementRule`) e `if` estaban acopladas a listas estáticas globales de reglas en `:app`.
  * **Solución:** Se introdujo recursividad en `FormatContext` agregando `formatStatement(statement: Statement): String`. Las reglas delegaron directamente en `ctx.formatStatement(...)`, permitiendo formatear estructuras anidadas arbitrariamente sin dependencias globales.

* **Problema 2: Comandos de CLI Desarticulados:**  
  Los comandos del CLI no permitían seleccionar la versión de lenguaje de forma declarativa ni reportaban errores con formato amigable.
  * **Solución:** Se implementó `ConfigFactory` en `:app` para ensamblar el pipeline completo según `LanguageVersion` (`V1_0` o `V1_1`), y se completaron los comandos `run` y `validate` en `CLI.kt`.

---

## 3. Arquitectura del Pipeline Refactorizado

```mermaid
flowchart TD
    subgraph Config [Configuración Versionada]
        LV[LanguageVersion: V1_0 | V1_1] --> CF[ConfigFactory]
    end

    subgraph FrontEnd [Análisis Léxico y Sintáctico]
        Tokens[CncTokens canónicos en :token] --> Lexer[Lexer]
        Lexer --> Stream[Cursor de Tokens]
        Stream --> PRules[StatementRulesBuilder]
        PRules --> Parser[Parser Predictivo LL 2]
        Parser --> AST[AST Tipado Inmutable]
    end

    subgraph Semantic [Análisis Semántico Jerárquico]
        AST --> SA[SemanticAnalyzer]
        SA --> ST[SymbolTable Jerárquica con Scopes]
        SA --> TR[ExpressionTypeResolver]
    end

    subgraph Runtime [Ejecución]
        AST --> Interp[Interpreter]
        Interp --> Env[Environment Jerárquico]
        Interp --> Ops[NumberOperations / StandardUnaryOperations]
        Interp --> Builtins[Builtins: println, readInput, readEnv]
    end

    CF --> Lexer
    CF --> Parser
    CF --> SA
    CF --> Interp
```

---

## 4. Matriz de Cambios por Módulo

| Módulo | Archivos Modificados / Creados | Responsabilidad |
| :--- | :--- | :--- |
| **`:common`** | `LanguageVersion.kt` [NEW], `Node.kt`, `ResultTest.kt` [NEW] | Enumeración oficial de versiones y verificación de utilidades base. |
| **`:token`** | `CncTokens.kt` [NEW] | Definiciones canónicas de keywords, símbolos y patrones accesibles por todos los módulos. |
| **`:parser`** | `StatementRule.kt`, `StandardStatementRules.kt`, `ExpressionBuilder.kt`, `ParserTest.kt` | Builders de reglas, soporte para `TokenDefinition`, erradicación de `error(...)` y tests de builders. |
| **`:semantic`** | `SymbolTable.kt`, `SemanticAnalyzer.kt`, `StandardExpressionTypeRules.kt`, `SemanticAnalyzerTest.kt` | Scopes léxicos jerárquicos, control de inmutabilidad en `const`, validación booleana de `if`. |
| **`:interpreter`**| `UnaryOperation.kt` [NEW], `StandardUnaryOperations.kt` [NEW], `NumberOperations.kt`, `InterpreterPresets.kt`, `ExpressionEvaluators.kt`, `InterpreterTest.kt` | Desacoplamiento de unarios, centralización numérica, presets v1.0 y v1.1 con `readInput`/`readEnv`. |
| **`:formatter`** | `FormatContext.kt`, `Formatter.kt` | Recursión de formateo en bloques e ifs sin acoplamiento a listas globales. |
| **`:app`** | `ConfigFactory.kt` [NEW], `Tokens.kt`, `CLI.kt`, `Formatter.kt`, `InterpreterConfig.kt`, `app.kt` | Fábrica de compiladores según versión, comandos CLI `run` y `validate`. |

---

## 5. Verificación de Calidad y Pruebas Automatizadas

Se ejecutó la suite completa de tests automatizados a través del wrapper de Gradle:
```bash
./gradlew test
```

### Resultados de la Suite:
* **Estado:** `BUILD SUCCESSFUL`
* **Tareas evaluadas:** 48 tareas ejecutadas / up-to-date.
* **Fallos / Regresiones:** 0 fallos.
* **Módulos verificados:**
  - `:common:test` $\rightarrow$ PASS
  - `:token:test` $\rightarrow$ PASS
  - `:lexer:test` $\rightarrow$ PASS
  - `:ast:test` $\rightarrow$ PASS
  - `:parser:test` $\rightarrow$ PASS (incluyendo rechazo de `const`/`if` en v1.0 y validación de `StatementRulesBuilder`)
  - `:semantic:test` $\rightarrow$ PASS (incluyendo scopes jerárquicos, shadowing, aislamiento de ramas y rechazo de reasignación a `const`)
  - `:interpreter:test` $\rightarrow$ PASS (incluyendo `readInput`, `readEnv`, operadores unarios y control de flujo)
  - `:formatter:test` $\rightarrow$ PASS
  - `:linter:test` $\rightarrow$ PASS
  - `:cli:test` $\rightarrow$ PASS
  - `:app:assemble` $\rightarrow$ PASS
