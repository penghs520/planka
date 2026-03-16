#!/usr/bin/env bash
#
# 单独启动指定的后端服务
#
# 用法:
#   ./start-service.sh schema-service
#   ./start-service.sh user-service
#   ./start-service.sh card-service
#   ./start-service.sh view-service
#   ./start-service.sh extension-service
#   ./start-service.sh oss-service
#   ./start-service.sh gateway-service
#

set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]:-$0}")" && pwd)"
PROJECT_ROOT="$(cd "${SCRIPT_DIR}/.." && pwd)"
LOGS_DIR="${PROJECT_ROOT}/logs"

# 颜色定义
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m'

log_info() { echo -e "${BLUE}[INFO]${NC} $1"; }
log_success() { echo -e "${GREEN}[OK]${NC} $1"; }
log_warn() { echo -e "${YELLOW}[WARN]${NC} $1"; }
log_error() { echo -e "${RED}[ERROR]${NC} $1"; }

# 获取服务信息的函数
get_service_info() {
    case "$1" in
        schema-service) echo "planka-services/schema-service:8081" ;;
        user-service) echo "planka-services/user-service:8082" ;;
        card-service) echo "planka-services/card-service:8083" ;;
        view-service) echo "planka-services/view-service:8084" ;;
        extension-service) echo "planka-services/extension-service:8085" ;;
        oss-service) echo "planka-services/oss/oss-service:8088" ;;
        gateway-service) echo "planka-services/gateway-service:8000" ;;
        *) echo "" ;;
    esac
}

# 列出所有服务
list_services() {
    echo "schema-service user-service card-service view-service extension-service oss-service gateway-service"
}

check_port() {
    lsof -Pi :"$1" -sTCP:LISTEN -t >/dev/null 2>&1
}

start_service() {
    local service_name="$1"

    if [ -z "$service_name" ]; then
        log_error "请指定服务名称"
        echo ""
        echo "可用的服务："
        for key in $(list_services); do
            echo "  - $key"
        done
        exit 1
    fi

    local service_info=$(get_service_info "$service_name")
    if [ -z "$service_info" ]; then
        log_error "未知的服务: $service_name"
        echo ""
        echo "可用的服务："
        for key in $(list_services); do
            echo "  - $key"
        done
        exit 1
    fi

    local service_path="${service_info%%:*}"
    local port="${service_info##*:}"
    local jar_path="${PROJECT_ROOT}/${service_path}/target/${service_name}.jar"

    # 检查 jar 文件是否存在
    if [ ! -f "$jar_path" ]; then
        log_error "找不到 jar 文件: $jar_path"
        log_info "请先运行构建: ./scripts/build.sh"
        exit 1
    fi

    # 检查端口是否已被占用
    if check_port "$port"; then
        log_warn "$service_name 已在端口 $port 上运行"
        local pid=$(lsof -Pi :"$port" -sTCP:LISTEN -t | head -1)
        log_info "进程 PID: $pid"
        echo ""
        read -p "是否重启该服务? (y/N) " -n 1 -r
        echo
        if [[ $REPLY =~ ^[Yy]$ ]]; then
            log_info "停止旧进程..."
            kill "$pid" 2>/dev/null || true
            sleep 2
            kill -9 "$pid" 2>/dev/null || true
            sleep 1
        else
            log_info "取消启动"
            exit 0
        fi
    fi

    # 创建日志目录
    mkdir -p "$LOGS_DIR"

    # 启动服务
    log_info "启动 $service_name (端口: $port)..."
    nohup java -jar \
        "-Dspring.application.name=${service_name}" \
        "-Dserver.port=${port}" \
        "-Dfile.encoding=UTF-8" \
        "$jar_path" \
        > "${LOGS_DIR}/${service_name}.log" 2>&1 &

    local java_pid=$!
    log_info "进程已启动，PID: $java_pid"

    # 等待服务启动
    log_info "等待服务启动..."
    local attempt=1
    while [ $attempt -le 30 ]; do
        if check_port "$port"; then
            log_success "$service_name 启动成功！"
            log_info "端口: $port"
            log_info "日志: ${LOGS_DIR}/${service_name}.log"
            echo ""
            log_info "查看日志: tail -f ${LOGS_DIR}/${service_name}.log"
            return 0
        fi
        sleep 2
        attempt=$((attempt + 1))
    done

    log_error "$service_name 启动超时"
    log_info "请查看日志: ${LOGS_DIR}/${service_name}.log"
    exit 1
}

# 主函数
main() {
    if [ "$1" = "-h" ] || [ "$1" = "--help" ]; then
        echo "用法: $0 <service-name>"
        echo ""
        echo "可用的服务："
        for key in $(list_services); do
            local info=$(get_service_info "$key")
            local port="${info##*:}"
            printf "  %-25s (端口: %s)\n" "$key" "$port"
        done
        echo ""
        echo "示例:"
        echo "  $0 schema-service"
        echo "  $0 gateway-service"
        exit 0
    fi

    start_service "$1"
}

main "$@"
