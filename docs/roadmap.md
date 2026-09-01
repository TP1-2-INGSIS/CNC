# Roadmap y Estado del Proyecto

Este documento registra el estado de avance y los hitos planificados para el motor de **PrintScript (CNC)**, sus componentes, versiones del lenguaje y requerimientos de calidad.

---

## PrintScript v1.0

Versión base del lenguaje con tipos primitivos, operaciones aritméticas, declaraciones de variables y salida por consola.

### 1. Token & Lexer
- [x] Abstracción de lectura por flujo continuo (`CharStream` / `ContentManager`)
- [x] Reconocimiento de caracteres por Trie optimizado (single-pass)
- [x] Palabras clave (`let`, `println`)
- [x] Tipos de datos (`string`, `number`)
- [x] Operadores aritméticos (`+`, `-`, `*`, `/`) y asignación (`=`)
- [x] Delimitadores y puntuación (`;`, `:`, `(`, `)`)
- [x] Literales:
  - [x] Literales de cadena (`"..."` y `'...'`)
  - [x] Literales numéricos enteros y decimales (`123`, `45.67`)
- [x] Manejo de identificadores válidos
- [x] Tracking preciso de posición en código (`Line`, `Column`)
- [x] Manejo de errores léxicos descriptivos

### 2. AST & Parser
- [x] Jerarquía de nodos AST (Declaraciones, Asignaciones, Llamadas a función, Expresiones binarias)
- [x] Analizador sintáctico modular basado en gramáticas
- [x] Parsing de declaración de variables (`let x: number = 5;` y `let x: string;`)
- [x] Parsing de asignación a variables existentes (`x = 10;`)
- [x] Parsing de llamadas a función (`println(...)`)
- [x] Precedencia de operadores aritméticos (`*`, `/` sobre `+`, `-`) y paréntesis
- [x] Manejo de errores sintácticos

### 3. Interpreter
- [ ] Entorno de ejecución y Scope / Tabla de símbolos
- [ ] Evaluación de expresiones aritméticas
- [ ] Concatenación de cadenas y coerción de tipos (`string + number`)
- [ ] Asignación y mutabilidad de variables `let`
- [ ] Ejecución de `println` redirigible a streams de salida (`IOManager`)
- [ ] Manejo de errores en tiempo de ejecución (variables no inicializadas, división por cero, etc.)

### 4. CLI (Command Line Interface)
- [x] Arquitectura de comandos (`Command`, `ArgsContainer`, `IOManager`)
- [x] Sistema de ayuda y documentación dinámica (`HelpAttribute`, `help [cmd]`)
- [x] REPL interactivo con comandos `exit` / `quit`
- [ ] Comando de validación (`validate <file> <version>`)
- [ ] Comando de ejecución directa (`run <file> <version>`)

---

## PrintScript v1.1 (Próxima Fase)

Extensión del lenguaje con estructuras de control, inmutabilidad y funciones de entrada.

### 1. Nuevas Características del Lenguaje
- [ ] Inmutabilidad de variables (`const`)
- [ ] Tipo de dato booleano (`boolean`, literales `true` / `false`)
- [ ] Estructuras de control condicional (`if` / `else`)
- [ ] Entradas de usuario (`readInput("prompt")`)
- [ ] Variables de entorno (`readEnv("VAR_NAME")`)

### 2. Herramientas Adicionales
- [ ] **Formatter**: Formateador de código con reglas configurables (JSON/YAML)
  - [ ] Espaciado alrededor de operadores y dos puntos
  - [ ] Saltos de línea antes y después de sentencias
  - [ ] Indentación en bloques condicionales
- [ ] **Linter**: Análisis estático de código
  - [ ] Validación de convenciones de nombres (`camelCase` / `snake_case`)
  - [ ] Restricción de llamadas complejas dentro de `println`
  - [ ] Detección de variables no utilizadas o redundantes

---

## Calidad de Código, Pruebas y DevOps

- [x] Arquitectura multi-módulo en Gradle con Kotlin DSL
- [x] Configuración centralizada de **JaCoCo** con umbral del **80%** de cobertura
- [x] Generación automatizada de reportes HTML por módulo en `check` / `test`
- [ ] Pipeline de Integración Continua (GitHub Actions)
- [ ] Suite de pruebas de integración end-to-end (scripts completos `.prs`)
- [ ] Linter estático para el código fuente Kotlin (ktlint / detekt)
