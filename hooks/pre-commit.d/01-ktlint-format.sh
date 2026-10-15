#!/usr/bin/env bash

set -e

REPO_ROOT="$(git rev-parse --show-toplevel 2>/dev/null || pwd)"
cd "$REPO_ROOT"

# Find staged Kotlin files (.kt and .kts) excluding deleted files
STAGED_KOTLIN_FILES=$(git diff --cached --name-only --diff-filter=d | grep -E '\.(kt|kts)$' || true)

if [ -z "$STAGED_KOTLIN_FILES" ]; then
    exit 0
fi

echo "=========================================="
echo "[INFO] Running ktlintFormat on staged Kotlin files..."
echo "=========================================="

if [ -f "./gradlew" ]; then
    ./gradlew ktlintFormat --no-daemon
elif [ -f "./gradlew.bat" ]; then
    cmd.exe /c gradlew.bat ktlintFormat --no-daemon
else
    gradle ktlintFormat --no-daemon
fi

FORMAT_EXIT_CODE=$?

if [ $FORMAT_EXIT_CODE -ne 0 ]; then
    echo ""
    echo "[ERROR] ktlintFormat failed with exit code $FORMAT_EXIT_CODE."
    exit $FORMAT_EXIT_CODE
fi

# Re-stage formatted files
for file in $STAGED_KOTLIN_FILES; do
    if [ -f "$file" ]; then
        git add "$file"
    fi
done

echo ""
echo "[SUCCESS] ktlintFormat completed and formatted files were re-staged."

exit 0
