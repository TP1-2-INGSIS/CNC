# Módulo: :linter

**Ruta:** `/linter`
**Dependencias directas:** `:common`, `:ast` (+ `gson` para parseo de configuración)
**Consumidores:** `:app`

---

## 1. Responsabilidad y Propósito (SRP)
- **Qué problema resuelve:** Realiza análisis estático de estilo sobre el AST ya parseado, emitiendo advertencias (*warnings*) sobre convenciones de código configurables, sin modificar el programa ni interrumpir su ejecución.
- **Qué hace:**
  - Recorre recursivamente el AST (incluyendo bloques anidados y ramas de `if`/`else`) aplicando un conjunto de reglas (`LinterRule`).
  - Provee reglas estándar: convención de nombres (`NamingConventionRule`) y restricción de llamadas con solo variable o literal (`SimpleFunctionCallRule`).
  - Construye el linter a partir de una configuración JSON (`LinterFactory`), inyectando validadores de nombres desacoplados (`NamingValidator`).
  - Provee validadores estándar: `CamelCaseValidator` y `SnakeCaseValidator`.
- **Qué NO hace (Fronteras):**
  - No modifica el código fuente ni el AST (eso es responsabilidad del `:formatter`).
  - No valida tipos ni semántica (responsabilidad de `:semantic`).
  - No detiene el pipeline: acumula advertencias y las retorna como una lista de mensajes.

---

## 2. Arquitectura y Componentes Clave

### Diagrama de Componentes
```mermaid
flowchart TD
    JSON["Config JSON (identifier_format, mandatory-variable-or-literal-*)"] --> Factory["LinterFactory.build()"]
    Validators["Map<String, NamingValidator>"] --> Factory
    Factory --> Linter["Result: Success(CNCLinter) / Failure"]
```

### Entidades de Dominio e Interfaces
1. **`CNCLinter`:**
   - Orquestador: `fun lint(statements: Sequence<Statement>): List<String>`.
   - Recorre el AST de forma recursiva (`checkRecursively`), descendiendo en `BlockStatement` y en las ramas `thenBlock` / `elseBlock` de `IfStatement`.
2. **`LinterRule`:**
   - Interfaz: `fun check(statement: Statement): List<String>`. Devuelve las advertencias que aplican a ese statement (o lista vacía).
3. **`NamingConventionRule`:**
   - Valida que las `Declaration` cumplan una convención de nombres delegando en un `NamingValidator` inyectado.
4. **`SimpleFunctionCallRule`:**
   - Verifica por composición que las llamadas a una función objetivo (ej: `println`, `readInput`) solo reciban variables o literales directos (sin expresiones complejas).
5. **`NamingValidator` (fun interface):**
   - Contrato `fun isValid(name: String): Boolean`. Implementaciones estándar: `CamelCaseValidator`, `SnakeCaseValidator`. Extensible con validadores propios.
6. **`LinterFactory`:**
   - Construye un `Result<CNCLinter>` desde un JSON resolviendo el validador de nombres por clave contra el mapa de validadores inyectado y habilitando las reglas correspondientes.

---

## 3. Manejo de Errores y Pipeline Funcional
- **Construcción del Linter:** `LinterFactory.build` retorna `Result<CNCLinter>`. Si la configuración contiene una convención desconocida o el JSON es inválido, retorna `Failure` en lugar de arrojar excepciones.
- **Modelo de salida del análisis:** El linter genera una `List<String>` de advertencias. Una lista vacía significa "sin observaciones".

---

## 4. Ejemplo Práctico Autónomo (End-to-End)

```kotlin
package example

import cnc.ast.*
import cnc.linter.*

fun main() {
    // 1. Configurar linter desde JSON, inyectando validadores disponibles
    val validators = mapOf(
        "camelCase" to CamelCaseValidator(),
        "snake_case" to SnakeCaseValidator()
    )

    val configJson = """
        {
            "naming-convention": "camelCase",
            "simple-println": true
        }
    """.trimIndent()

    val linterResult = LinterFactory.build(configJson, validators)
    if (linterResult !is Success) return

    val linter = linterResult.data

    // 2. Analizar un AST con dos infracciones
    val programa = sequenceOf<Statement>(
        Declaration("bad_name", "number", NumberLiteral(1.0)),           // no es camelCase
        Call("println", listOf(BinaryExpression(Identifier("a"), "*", Identifier("b")))) // expresión compleja
    )

    val advertencias = linter.lint(programa)
    advertencias.forEach { println("WARN: $it") }
    // WARN: La variable 'bad_name' debería estar en formato camelCase.
    // WARN: La llamada a 'println' no permite expresiones complejas.

    // 3. Validador personalizado inyectado (notación húngara)
    val custom = mapOf<String, NamingValidator>("hungarian" to NamingValidator { it.startsWith("m_") })
    val linterCustomResult = LinterFactory.build("""{ "naming-convention": "hungarian" }""", custom)
    if (linterCustomResult is Success) {
        println(linterCustomResult.data.lint(sequenceOf(Declaration("count", "number", NumberLiteral(1.0)))))
    }
}
```

---

## 5. Integración en el Pipeline del Compilador
- **Entrada:** `Sequence<Statement>` producida por el `:parser` (opcionalmente ya validada por `:semantic`).
- **Salida:** `List<String>` de advertencias reportadas al usuario. Es una herramienta lateral: no forma parte del flujo lexer → parser → semantic → interpreter, sino que consume el AST en paralelo.

---

## 6. Decisiones Técnicas y Tradeoffs
- **Validadores inyectados vs hardcodeados:** `NamingValidator` es una `fun interface` inyectada por mapa. Agregar una convención nueva (ej: `PascalCase`, notación húngara) no requiere tocar `NamingConventionRule` ni el motor.
- **Reglas componibles:** Cada `LinterRule` es autónoma y se agrega a la lista del `CNCLinter`. El conjunto activo se decide por configuración JSON, no en código.
- **Recorrido recursivo del AST:** El linter desciende en `BlockStatement` e `IfStatement` (PrintScript 1.1) para analizar sentencias anidadas, no solo el nivel superior.
- **Salida como lista de mensajes:** Se prioriza recolectar *todas* las advertencias en una pasada (no fail-fast), a diferencia del pipeline de compilación que se detiene ante el primer error.
