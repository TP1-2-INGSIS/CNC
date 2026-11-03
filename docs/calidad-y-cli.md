# Calidad de código y CLI

## 1. Linter y formatter en todos los módulos, sin repetir el setup

**Pedido:** ktlint/detekt en todos los módulos, configurados en un solo lugar.
**Solución:** convention plugin en `buildSrc/src/main/kotlin/code-quality-conventions.gradle.kts`. Cada módulo solo lo declara:

```kotlin
plugins {
    id("jacoco-conventions")
    id("code-quality-conventions")
}
```

```bash
./gradlew ktlintCheck   # verifica formato
./gradlew ktlintFormat  # formatea
./gradlew detekt        # linter
```

## 2. Coverage mayor al 80% en todos los módulos

**Pedido:** más de 80% de cobertura en todos los módulos.
**Solución:** plugin `jacoco-conventions`, que hace fallar el build si algún módulo baja del 80%, y tests unitarios en todos los módulos (PR #80). `ast` está excluido porque solo tiene data classes.

```kotlin
violationRules { rule { limit { minimum = "0.80".toBigDecimal() } } }
```

## 3. `build` corre el linter

**Pedido:** que `./gradlew build` también corra el linter.
**Solución:** `build` depende de `check`, y el plugin engancha ktlint y detekt a `check`:

```kotlin
tasks.matching { it.name == "check" }.configureEach {
    dependsOn(tasks.matching { it.name == "ktlintCheck" })
    dependsOn(tasks.matching { it.name == "detekt" })
}
```

## 4. CLI con todas las operaciones

**Pedido:** un CLI que corra todas las operaciones (`run`, `lint`, `format`, etc.) y al que se le pueda pasar la config del linter y del formatter.
**Solución:** CLI interactivo con los comandos `run`, `validate`, `lint`, `format` y `help` (PR #89). La config se pasa como la ruta a un JSON.

```
./gradlew :app:run
-> $ run ../samples/clean_code.ps --version=1.0
-> $ lint ../samples/lint_test.ps --config=../samples/lint-config.json
-> $ format ../samples/unformatted.ps --config=../samples/format-config.json
```

- Las opciones van como `--clave=valor`, sin espacio.
- Las rutas relativas se resuelven desde `app/`.
- `format` sobrescribe el archivo.
