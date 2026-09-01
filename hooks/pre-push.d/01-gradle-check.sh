#!/usr/bin/env bash

set -e

REPO_ROOT="$(git rev-parse --show-toplevel 2>/dev/null || pwd)"
cd "$REPO_ROOT"

echo "=========================================="
echo "[INFO] Running './gradlew check' before push..."
echo "=========================================="

if [ -f "./gradlew" ]; then
    ./gradlew check
elif [ -f "./gradlew.bat" ]; then
    cmd.exe /c gradlew.bat check
else
    gradle check
fi

EXIT_CODE=$?

if [ $EXIT_CODE -ne 0 ]; then
    echo ""
    echo "[ERROR] Pre-push check failed: './gradlew check' returned errors."
    echo "        Please fix the issues (linting, tests, etc.) before pushing."
    exit $EXIT_CODE
else
    echo ""
    echo "[SUCCESS] Pre-push check passed successfully."
fi

exit 0
