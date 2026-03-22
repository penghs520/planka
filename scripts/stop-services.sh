#!/bin/bash

# 停止 planka 所有 Java 服务
# 支持按端口或按应用名称停止

# 服务端口号列表
# 与 planka-dev.sh 中各服务端口对应（18xxx → 8xxx，便于与常见本地端口区分）
PORTS=(8000 8100 8101 8102 8103 8104 8105 8106)

# 服务名称列表
SERVICE_NAMES=(
  "gateway-service"
  "schema-service"
  "card-service"
  "user-service"
  "view-service"
  "oss-service"
  "history-service"
  "comment-service"
)

# 颜色定义
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# 根据端口停止进程
stop_by_port() {
  local port=$1
  local pid=$(lsof -ti :$port 2>/dev/null)

  if [ -n "$pid" ]; then
    echo -e "${YELLOW}Stopping process on port $port (PID: $pid)...${NC}"
    kill -15 $pid 2>/dev/null
    sleep 2

    # 检查是否仍在运行
    if kill -0 $pid 2>/dev/null; then
      echo -e "${RED}Force killing process on port $port (PID: $pid)...${NC}"
      kill -9 $pid 2>/dev/null
    fi
    echo -e "${GREEN}Port $port stopped${NC}"
    return 0
  else
    echo "Port $port is not in use"
    return 1
  fi
}

# 根据服务名称停止进程
stop_by_name() {
  local service_name=$1
  local pid=$(pgrep -f "java.*$service_name" 2>/dev/null)

  if [ -n "$pid" ]; then
    echo -e "${YELLOW}Stopping $service_name (PID: $pid)...${NC}"
    kill -15 $pid 2>/dev/null
    sleep 2

    # 检查是否仍在运行
    if kill -0 $pid 2>/dev/null; then
      echo -e "${RED}Force killing $service_name (PID: $pid)...${NC}"
      kill -9 $pid 2>/dev/null
    fi
    echo -e "${GREEN}$service_name stopped${NC}"
    return 0
  else
    echo "$service_name is not running"
    return 1
  fi
}

# 停止所有服务
stop_all() {
  echo "=== Stopping all planka services ==="
  local stopped=0

  for port in "${PORTS[@]}"; do
    if stop_by_port $port; then
      ((stopped++))
    fi
  done

  echo ""
  echo -e "${GREEN}Total services stopped: $stopped${NC}"
}

# 停止指定端口的服务
stop_specific_port() {
  local port=$1
  echo "=== Stopping service on port $port ==="
  stop_by_port $port
}

# 停止指定名称的服务
stop_specific_service() {
  local service=$1
  echo "=== Stopping service: $service ==="
  stop_by_name $service
}

# 显示使用帮助
show_help() {
  cat << EOF
Usage: $0 [option]

Options:
  (no args)       Stop all services
  -p, --port PORT Stop service on specific port
  -n, --name NAME Stop service by name
  -l, --list      List all services and ports
  -h, --help      Show this help message

Examples:
  $0                      # Stop all services
  $0 -p 8000              # Stop gateway service
  $0 -n gateway-service   # Stop gateway service by name
EOF
}

# 列出所有服务
list_services() {
  echo "=== planka Services ==="
  printf "%-25s %s\n" "Service Name" "Port"
  echo "----------------------------------------"
  for i in "${!SERVICE_NAMES[@]}"; do
    printf "%-25s %d\n" "${SERVICE_NAMES[$i]}" "${PORTS[$i]}"
  done
}

# 主逻辑
case "${1:-}" in
  -p|--port)
    if [ -z "${2:-}" ]; then
      echo "Error: Port number required"
      exit 1
    fi
    stop_specific_port "$2"
    ;;
  -n|--name)
    if [ -z "${2:-}" ]; then
      echo "Error: Service name required"
      exit 1
    fi
    stop_specific_service "$2"
    ;;
  -l|--list)
    list_services
    ;;
  -h|--help)
    show_help
    ;;
  "")
    stop_all
    ;;
  *)
    echo "Unknown option: $1"
    show_help
    exit 1
    ;;
esac
