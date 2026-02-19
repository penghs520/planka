#!/bin/bash

# 配置 Git 使用项目的 hooks 目录
# 运行此脚本后，pre-commit hook 将自动生效

set -e

ROOT_DIR="$(git rev-parse --show-toplevel)"

echo "Setting up git hooks..."
git config core.hooksPath "$ROOT_DIR/.githooks"

echo "✅ Git hooks configured successfully!"
echo "Pre-commit hook will now run before each commit."
