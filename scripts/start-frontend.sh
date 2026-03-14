#!/usr/bin/env bash
#
# 启动前端开发服务器
#
# 用法:
#   ./start-frontend.sh
#

set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]:-$0}")" && pwd)"
PROJECT_ROOT="$(cd "${SCRIPT_DIR}/.." && pwd)"
FRONTEND_DIR="${PROJECT_ROOT}/kanban-ui"

# 颜色定义
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
CYAN='\033[0;36m'
NC='\033[0m'

log_info() { echo -e "${BLUE}[INFO]${NC} $1"; }
log_success() { echo -e "${GREEN}[OK]${NC} $1"; }
log_warn() { echo -e "${YELLOW}[WARN]${NC} $1"; }
log_error() { echo -e "${RED}[ERROR]${NC} $1"; }
log_step() {
    echo -e "\n${CYAN}========================================${NC}"
    echo -e "${CYAN}  $1${NC}"
    echo -e "${CYAN}========================================${NC}\n"
}

# 检查 pnpm 是否安装
if ! command -v pnpm &> /dev/null; then
    log_error "pnpm 未安装"
    log_info "请安装 pnpm: npm install -g pnpm"
    exit 1
fi

# 检查前端目录
if [ ! -d "$FRONTEND_DIR" ]; then
    log_error "前端目录不存在: $FRONTEND_DIR"
    exit 1
fi

cd "$FRONTEND_DIR"

# 检查 node_modules
if [ ! -d "node_modules" ]; then
    log_step "安装依赖"
    pnpm install
fi

log_step "启动前端开发服务器"

log_info "前端地址: ${GREEN}http://localhost:3000${NC}"
log_info "API 代理: ${GREEN}http://localhost:8000${NC}"
echo ""
log_info "按 Ctrl+C 停止服务器"
echo ""

# 启动开发服务器
pnpm dev
