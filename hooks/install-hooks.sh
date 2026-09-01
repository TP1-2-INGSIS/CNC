#!/usr/bin/env bash

set -e

REPO_ROOT="$(git rev-parse --show-toplevel 2>/dev/null || pwd)"
cd "$REPO_ROOT"

echo "Configuring Git hooks path to 'hooks'..."
git config core.hooksPath hooks

# Ensure hook files are executable on Unix-like environments
chmod +x hooks/pre-push hooks/pre-commit hooks/post-commit 2>/dev/null || true
chmod +x hooks/pre-push.d/*.sh hooks/pre-commit.d/*.sh hooks/post-commit.d/*.sh 2>/dev/null || true

echo "[SUCCESS] Git hooks configured successfully!"
