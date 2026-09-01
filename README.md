# CNC - PrintScript Engine

Motor de análisis, validación y ejecución para el lenguaje **PrintScript**, desarrollado en **Kotlin** bajo una arquitectura multi-módulo altamente desacoplada y orientada a la extensibilidad.

---

## Filosofía de Diseño

El propósito fundamental de **CNC** es funcionar como un conjunto de herramientas y bloques de construcción que permitan a cualquier desarrollador construir o extender un lenguaje de programación de manera simple y declarativa:

- **Bloques de construcción modulares**: Cada componente (lectura de streams, tokenización, gramáticas sintácticas, árbol de sintaxis abstracta y ejecución) está desacoplado y listo para usar como una pieza independiente.
- **Foco en lo conceptual**: El desarrollador no necesita implementar algoritmos complejos de buffers, control de posiciones o traversals desde cero. Solo define los conceptos esenciales de su lenguaje: **palabras clave, tipos de token, reglas gramaticales y semántica de ejecución**.
- **Extensibilidad sin fricción**: Permite incorporar nuevas sintaxis, evolucionar versiones del lenguaje (como la transición de PrintScript 1.0 a 1.1) o crear lenguajes totalmente nuevos inyectando configuraciones sin modificar el núcleo de los motores existentes.

---

## Índice de Documentación

La documentación detallada del proyecto se encuentra organizada en el directorio `docs/`:

| Documento | Descripción |
| :--- | :--- |
| [Roadmap y Estado](docs/roadmap.md) | Seguimiento de avance con checklists para PrintScript v1.0, v1.1 y calidad. |
| [Arquitectura del Sistema](docs/architecture.md) | Diagrama de flujo, responsabilidades por submódulo y decisiones de diseño. |
| [Guía de Desarrollo](docs/development.md) | Prerrequisitos, compilación, ejecución de tests, cobertura con JaCoCo y CLI. |
| [Especificación del Lenguaje](docs/specification.md) | Sintaxis formal, tipos de datos, operadores y sentencias de PrintScript. |

---

## Módulos del Proyecto

El proyecto está compuesto por los siguientes submódulos en Gradle:

- **`:common`**: Abstracciones transversales, gestión de posiciones (`Position`), flujos de texto (`CharStream`) y tipo funcional `Result<T>`.
- **`:token`**: Definición y categorización de tokens del lenguaje.
- **`:lexer`**: Analizador léxico streaming single-pass con estructura Trie y reglas configurables.
- **`:ast`**: Jerarquía de nodos del árbol de sintaxis abstracta.
- **`:parser`**: Analizador sintáctico generador del AST basado en gramáticas modulares.
- **`:interpreter`**: Motor de evaluación, scopes y ejecución del AST.
- **`:cli`**: Interfaz de línea de comandos, comandos desacoplados y abstracción de I/O (`IOManager`).
- **`:app`**: Punto de entrada principal y orquestador del compilador.

---

## Inicio Rápido

### Prerrequisitos
- Java JDK 21+

### Comandos de Gradle

```bash
# Compilar todo el proyecto
./gradlew build

# Ejecutar todos los tests unitarios
./gradlew test

# Ejecutar verificación completa (Tests + Verificación de Cobertura JaCoCo >= 80%)
./gradlew check

# Ejecutar la aplicación
./gradlew :app:run
```

Para más detalles sobre opciones de ejecución y parámetros, consultar la [Guía de Desarrollo](docs/development.md).
