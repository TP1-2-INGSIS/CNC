# Guía de Desarrollo y Ejecución

Guía para desarrolladores sobre cómo configurar el entorno, compilar, probar y ejecutar el motor de PrintScript (CNC).

---

## Prerrequisitos

- **Java JDK**: Versión 21 o superior.
- **Gradle**: Se incluye el wrapper de Gradle (`./gradlew` o `.\gradlew.bat`), por lo que no es necesario instalarlo globalmente.
- **Git**: Para el control de versiones y flujo de trabajo en ramas.

---

## Comandos Principales de Gradle

### 1. Compilación
Compila todos los submódulos del proyecto:

```bash
# Linux / macOS
./gradlew build

# Windows (PowerShell)
.\gradlew.bat build
```

### 2. Ejecución de Tests
Ejecuta la suite de pruebas unitarias de todos los submódulos:

```bash
# Linux / macOS
./gradlew test

# Windows (PowerShell)
.\gradlew.bat test
```

Para correr los tests de un módulo específico (por ejemplo, `:lexer`):

```bash
./gradlew :lexer:test
```

### 3. Verificación de Calidad y Cobertura (JaCoCo)
Ejecuta las pruebas, genera los reportes de JaCoCo y valida que se alcance el **umbral mínimo de cobertura del 80%**:

```bash
# Linux / macOS
./gradlew check

# Windows (PowerShell)
.\gradlew.bat check
```

Los reportes HTML generados quedan disponibles en la siguiente ruta dentro de cada módulo:
```
<nombre-modulo>/build/reports/jacoco/test/html/index.html
```

### 4. Ejecución de la Aplicación
Para ejecutar el punto de entrada principal del proyecto:

```bash
# Linux / macOS
./gradlew :app:run

# Windows (PowerShell)
.\gradlew.bat :app:run
```

---

## Estructura del Código Fuente

Cada módulo sigue la convención estándar de Gradle / Kotlin:

```
<modulo>/
├── build.gradle.kts
└── src/
    ├── main/
    │   └── kotlin/cnc/...
    └── test/
        └── kotlin/cnc/...
```

---

## Convenciones de Contribución y CI/CD

El proyecto sigue estándares rigurosos de ingeniería de software automatizados mediante pipelines de CI/CD:

- **Guía de Contribución**: Consultar [CONTRIBUTING.md](../CONTRIBUTING.md) para detalles sobre convenciones de ramas, formato estricto de **Conventional Commits** y principios de diseño arquitectónico.
- **Arquitectura de CI/CD y Semantic Release**: Consultar [docs/ci-cd.md](ci-cd.md) para entender el flujo de verificación continua en PRs, el cálculo automático de versiones SemVer y la publicación de artefactos en GitHub Packages y GitHub Releases.
- **Compuertas de Calidad**: Todo PR debe superar `./gradlew check` (compilación + ktlint + detekt + JUnit 5 + JaCoCo $\ge$ 80%).

