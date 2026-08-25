# Git Hooks

Este directorio contiene los Git hooks del proyecto organizados de forma modular.

## Estructura

- `pre-push`: Hook dispatcher que ejecuta todos los scripts dentro de `pre-push.d/`.
- `pre-commit`: Hook dispatcher que ejecuta todos los scripts dentro de `pre-commit.d/`.
- `post-commit`: Hook dispatcher que ejecuta todos los scripts dentro de `post-commit.d/`.
- `pre-push.d/`: Contiene los scripts que se ejecutan antes de realizar `git push` (ej: `01-gradle-check.sh` que corre `./gradlew check`).
- `pre-commit.d/`: Subcarpeta para futuros scripts de `pre-commit`.
- `post-commit.d/`: Subcarpeta para futuros scripts de `post-commit`.

## Instalacion

Para activar estos hooks en tu entorno local de Git:

### Linux / macOS / Git Bash
```bash
./hooks/install-hooks.sh
```

### Windows (CMD / PowerShell)
```cmd
.\hooks\install-hooks.bat
```

O manualmente ejecutando:
```bash
git config core.hooksPath hooks
```

## Como agregar un nuevo hook

1. Coloca tu script (`.sh`) dentro de la subcarpeta correspondiente (`pre-commit.d/`, `pre-push.d/`, `post-commit.d/`, etc.).
2. Asegurate de que el script devuelva un codigo de salida `0` en caso de exito y distinto de `0` en caso de fallo para abortar la operacion de Git cuando sea necesario.
