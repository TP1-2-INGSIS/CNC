# Guía de Contribución y Estándares de Ingeniería

Este documento establece las políticas de desarrollo, estándares de código, convenciones de commit y compuertas de calidad que rigen en **CNC (PrintScript Engine)**. Todo colaborador del proyecto debe seguir estas directrices de manera obligatoria.

---

## 1. Estrategia de Branching y Flujo de Trabajo

* **Rama Principal (`main`):**
  * Representa el **mainline** de integración continua.
  * **Invariante no negociable (La Regla del Rojo):** `main` debe estar **siempre en verde**. Está terminantemente prohibido integrar cambios que rompan el build, las pruebas o la cobertura.
* **Feature Branches Cortas:**
  * Toda funcionalidad o corrección se trabaja en una rama derivada de `main` con nombres semánticos:
    * `feat/<nombre-funcionalidad>`
    * `fix/<nombre-error>`
    * `refactor/<alcance>`
  * **Baja Divergencia:** Mantener las ramas sincronizadas con `main` frecuentemente para evitar conflictos tardíos.
  * Ninguna rama vive más de uno o dos días sin integrarse al mainline.

---

## 2. Convención de Commits y Títulos de Pull Request

El proyecto implementa **Conventional Commits 1.0.0**. Esta convención no es puramente estética: **alimenta directamente el pipeline automatizado de Semantic Versioning (SemVer) en CI/CD**.

### Estructura Canónica
```text
<tipo>(<alcance opcional>): <descripción concisa en minúsculas>

[cuerpo explicativo opcional]

[pie(s) de página opcional(es)]
```

### Tipos Permitidos y su Impacto en el Versionado

| Tipo | Propósito | Impacto SemVer en Release |
| :--- | :--- | :---: |
| **`fix`** | Corrige un defecto sin modificar el contrato observable. | **PATCH** (`1.3.0` $\rightarrow$ `1.3.1`) |
| **`feat`** | Incorpora una nueva funcionalidad retrocompatible. | **MINOR** (`1.3.0` $\rightarrow$ `1.4.0`) |
| **`feat!`** / **`fix!`**<br>o con `BREAKING CHANGE:` | Modifica o rompe la interfaz pública o el AST. | **MAJOR** (`1.3.0` $\rightarrow$ `2.0.0`) |
| **`refactor`** | Refactorización interna sin cambio funcional ni bugfix. | *No genera release* |
| **`test`** | Agrega o refactoriza pruebas automatizadas. | *No genera release* |
| **`docs`** | Modificaciones exclusivas en documentación. | *No genera release* |
| **`style`** | Ajustes de formato visual (espacios, indentación). | *No genera release* |
| **`build`** / **`ci`** | Modificaciones en scripts de Gradle o workflows. | *No genera release* |
| **`chore`** | Mantenimiento rutinario de dependencias o herramientas. | *No genera release* |

### Reglas Estrictas
1. El asunto del commit o título del PR debe comenzar con el tipo en **minúsculas** (ej. `feat(parser): add support for const`).
2. Debe estar redactado en modo imperativo y no terminar con punto.
3. Si introduce un cambio que rompe compatibilidad (Breaking Change), debe colocarse un signo de exclamación antes de los dos puntos (`feat!:` o `feat(parser)!:`) o incluir en el pie de página:
   ```text
   BREAKING CHANGE: se modifica la firma del método parseExpression() en la interfaz Parser.
   ```

---

## 3. Compuertas de Calidad Local y Pre-Commit

Antes de abrir un Pull Request, el desarrollador debe certificar que el código supera el 100% de las compuertas de calidad ejecutando:

```bash
# Linux / macOS
./gradlew check

# Windows (PowerShell)
.\gradlew.bat check
```

La orden `./gradlew check` ejecuta de forma monolítica y determinística:
1. **Compilación de código fuente:** Kotlin y Java con JDK 21.
2. **Formato (`ktlintCheck`):** Asegura el estándar estético acordado (puede corregirse automáticamente con `./gradlew ktlintFormat`).
3. **Análisis Estático (`detekt`):** Inspecciona el AST en busca de complejidad ciclomática excesiva, code smells, números mágicos y violaciones de convenciones.
4. **Tests Unitarios e Integrales:** Toda la suite de JUnit 5 sobre los 12 submódulos.
5. **Verificación de Cobertura (`jacocoTestCoverageVerification`):** Valida que el código alcance o supere el **80% de cobertura de instrucciones**.

> **Ventanas Rotas:** No se admiten tests comentados con `@Disabled` ni `@Ignore` para pasar el build. Cualquier fallo se resuelve en la causa raíz.

---

## 4. Filosofía Arquitectónica y Principios de Diseño

Al escribir código en cualquier módulo de CNC:

1. **Tipado Estático y Seguridad (Type-Safety):**
   * El AST se modela estrictamente mediante `sealed interface` y `data class` inmutables.
   * Prohibido el tipado débil o dispatch por strings (`if (tag == "Declaration")`). Siempre preferir pattern matching exhaustivo (`when`).
2. **Composición sobre Herencia:**
   * Evitar clases base con estado mutable compartido y métodos `open`.
   * Promover la composición mediante interfaces funcionales y estrategias (ej. reglas de parseo, reglas de lexer, evaluadores).
3. **Responsabilidad Única (SRP):**
   * Cada módulo responde a una única fase: `:lexer` tokeniza, `:parser` construye el AST, `:semantic` valida tipos y scopes, `:interpreter` ejecuta y genera efectos observables.
4. **Inmutabilidad en Datos vs. Mutabilidad Encapsulada:**
   * Todas las estructuras que viajan entre etapas (`Token`, `Statement`, `Expression`, `Result`) son inmutables (`val`).
   * La mutabilidad requerida para streams o cursores de lectura se mantiene estrictamente privada y encapsulada.

---

## 5. Ciclo de Pull Requests y CI/CD

1. **Apertura del PR:** El título del PR es validado automáticamente por el bot de CI (`amannn/action-semantic-pull-request`).
2. **Verificación Automática:** GitHub Actions ejecuta `./gradlew check` en un entorno limpio.
3. **Revisión de Código:** Se revisa diseño, legibilidad y cumplimiento de invariantes.
4. **Merge a Main:**
   * Al mergear, el workflow de CD analiza los commits del PR.
   * Calcula el incremento de versión según SemVer.
   * Genera los binarios ejecutables de la CLI (`distZip`, `distTar`).
   * Publica los paquetes Maven en **GitHub Packages**.
   * Crea el **Git Tag** y el **GitHub Release** formal con el changelog automático.
