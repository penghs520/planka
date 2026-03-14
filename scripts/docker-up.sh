#!/bin/bash
#
# docker-up.sh - 智能启动Docker服务，根据CPU架构自动选择镜像
#

set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "${SCRIPT_DIR}/.." && pwd)"

# 检测CPU架构
ARCH=$(uname -m)

# 根据架构设置Nacos镜像
if [[ "$ARCH" == "arm64" ]] || [[ "$ARCH" == "aarch64" ]]; then
    # Apple Silicon 或其他ARM64架构
    export NACOS_IMAGE="v2.3.0-slim"
    echo "[INFO] 检测到ARM64架构，使用Nacos镜像: nacos/nacos-server:$NACOS_IMAGE"
else
    # x86_64 或其他架构
    export NACOS_IMAGE="v2.3.0"
    echo "[INFO] 检测到x86_64架构，使用Nacos镜像: nacos/nacos-server:$NACOS_IMAGE"
fi

# 启动Docker服务
cd "$PROJECT_ROOT"
docker-compose up -d "$@"

echo "[OK] Docker服务启动完成"
echo ""
echo "服务地址:"
echo "  Nacos:  http://localhost:18848/nacos"
echo "  MySQL:  localhost:13306"
echo "  Redis:  localhost:16379"
echo "  Kafka:  localhost:19092"
