#!/usr/bin/env bash
#
# 停止指定的后端服务
#
# 用法:
#   ./stop-service.sh schema-service
#   ./stop-service.sh all  # 停止所有服务
#

set -e

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

# 获取服务端口
get_service_port() {
    case "$1" in
        schema-service) echo "8081" ;;
        user-service) echo "8082" ;;
        card-service) echo "8083" ;;
        view-service) echo "8084" ;;
        extension-service) echo "8085" ;;
        oss-service) echo "8088" ;;
        gateway-service) echo "8000" ;;
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

get_pid_by_port() {
    lsof -Pi :"$1" -sTCP:LISTEN -t 2>/dev/null | head -1
}

stop_service() {
    local service_name="$1"
    local port=$(get_service_port "$service_name")

    if [ -z "$port" ]; then
        log_error "未知的服务: $service_name"
        return 1
    fi

    if ! check_port "$port"; then
        log_warn "$service_name 未运行 (端口 $port)"
        return 0
    fi

    local pid=$(get_pid_by_port "$port")
    if [ -n "$pid" ]; then
        log_info "停止 $service_name (PID: $pid, 端口: $port)..."
        kill "$pid" 2>/dev/null || true
        sleep 1

        # 如果还在运行，强制杀死
        if check_port "$port"; then
            log_warn "正常停止失败，强制终止..."
            kill -9 "$pid" 2>/dev/null || true
            sleep 1
        fi

        if check_port "$port"; then
            log_error "$service_name 停止失败"
            return 1
        else
            log_success "$service_name 已停止"
        fi
    fi
}

stop_all_services() {
    log_info "停止所有服务..."
    for service_name in $(list_services); do
        stop_service "$service_name"
    done
    log_success "所有服务已停止"
}

show_status() {
    echo ""
    echo "服务状态："
    printf "%-25s %-10s %-10s\n" "服务名称" "端口" "状态"
    echo "--------------------------------------------------------"

    for service_name in $(list_services); do
        local port=$(get_service_port "$service_name")
        local status="${RED}停止${NC}"
        local pid=""

        if check_port "$port"; then
            pid=$(get_pid_by_port "$port")
            status="${GREEN}运行中${NC} (PID: $pid)"
        fi

        printf "%-25s %-10s %-10b\n" "$service_name" "$port" "$status"
    done
    echo ""
}

main() {
    if [ "$1" = "-h" ] || [ "$1" = "--help" ]; then
        echo "用法: $0 <service-name|all>"
        echo ""
        echo "可用的服务："
        for key in $(list_services); do
            local port=$(get_service_port "$key")
            printf "  %-25s (端口: %s)\n" "$key" "$port"
        done
        echo ""
        echo "特殊命令："
        echo "  all                       停止所有服务"
        echo ""
        echo "示例:"
        echo "  $0 schema-service"
        echo "  $0 all"
        exit 0
    fi

    if [ "$1" = "all" ]; then
        stop_all_services
        show_status
    elif [ -z "$1" ]; then
        log_error "请指定服务名称或 'all'"
        echo ""
        echo "可用的服务："
        for key in $(list_services); do
            echo "  - $key"
        done
        exit 1
    else
        stop_service "$1"
        show_status
    fi
}

main "$@"
