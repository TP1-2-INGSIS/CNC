#!/usr/bin/env bash
# Installs git hooks from scripts/ into .git/hooks/
# Usage: ./scripts/install-hooks.sh

set -e

HOOK_DIR="$(git rev-parse --show-toplevel)/.git/hooks"
SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"

echo "Installing git hooks..."

cp "$SCRIPT_DIR/pre-commit" "$HOOK_DIR/pre-commit"
chmod +x "$HOOK_DIR/pre-commit"

echo "✅ pre-commit hook installed in .git/hooks/"
echo ""
echo "To skip the hook on a specific commit, use: git commit --no-verify"
