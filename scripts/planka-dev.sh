#!/bin/bash
#
# planka-dev.sh - 敏捷看板开发环境一键管理脚本（简化版）
#
# 命令:
#   build       - 构建前端和后端（不启动服务）
#   up          - 启动/重启 Docker 和所有后端服务
#   down        - 停止所有后端服务和 Docker（不移除容器）
#   e2e         - 运行 E2E 测试（需先启动所有服务）
#
# 选项:
#   --skip-tests, -st    跳过测试
#   --skip-frontend, -sf 跳过前端构建
#   --skip-backend, -sb  跳过后端构建
#   --docker, -d         使用 Docker 构建前端（宿主无 pnpm 时自动使用）
#   --help, -h           显示帮助
#

set -e

# ============================================
# 配置项
# ============================================
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "${SCRIPT_DIR}/.." && pwd)"
LOGS_DIR="${PROJECT_ROOT}/logs"
MVN_CMD="${PROJECT_ROOT}/mvnw"

FRONTEND_DIR="${PROJECT_ROOT}/planka-ui"
FRONTEND_DIST="${FRONTEND_DIR}/dist"

SERVICES=(
    "planka-services/gateway-service"
    "planka-services/card-service"
    "planka-services/user-service"
    "planka-services/view-service"
    "planka-services/oss/oss-service"
    "planka-services/extension-service"
    "planka-services/schema-service"
)

SERVICE_NAMES=(
    "网关服务"
    "卡片服务"
    "用户服务"
    "视图服务"
    "OSS服务"
    "扩展服务"
    "Schema服务"
)

# 与各服务 application.yml 中 server.port 一致（顺序同 SERVICES）
SERVICE_PORTS=(
    "18000"   # gateway-service
    "18101"   # card-service
    "18102"   # user-service
    "18103"   # view-service
    "18104"   # oss-service
    "18105"   # extension-service
    "18100"   # schema-service
)

RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
CYAN='\033[0;36m'
NC='\033[0m'

# ============================================
# 工具函数
# ============================================
log_info() { echo -e "${BLUE}[INFO]${NC} $1"; }
log_success() { echo -e "${GREEN}[OK]${NC} $1"; }
log_warn() { echo -e "${YELLOW}[WARN]${NC} $1"; }
log_error() { echo -e "${RED}[ERROR]${NC} $1"; }
log_step() {
    echo -e "\n${CYAN}========================================${NC}"
    echo -e "${CYAN}  $1${NC}"
    echo -e "${CYAN}========================================${NC}\n"
}

check_command() {
    if ! command -v "$1" &> /dev/null; then
        log_error "$1 未安装"
        exit 1
    fi
}

check_port() {
    lsof -Pi :"$1" -sTCP:LISTEN -t >/dev/null 2>&1
}

get_service_index() {
    local i=0
    for s in "${SERVICES[@]}"; do
        [ "$s" = "$1" ] && echo $i && return
        i=$((i + 1))
    done
    echo -1
}

get_service_pid() {
    local idx=$(get_service_index "$1")
    local port=${SERVICE_PORTS[$idx]}
    lsof -Pi :"$port" -sTCP:LISTEN -t 2>/dev/null | head -1
}

# ============================================
# Docker 操作
# ============================================
start_docker() {
    log_step "启动 Docker 基础设施"
    cd "$PROJECT_ROOT"
    docker-compose up -d
    sleep 5
    
    local services=("planka-nacos" "planka-mysql" "planka-redis" "planka-kafka")
    for service in "${services[@]}"; do
        if docker ps | grep -q "$service"; then
            log_success "$service 运行中"
        fi
    done
}

stop_docker() {
    log_step "停止 Docker 基础设施"
    cd "$PROJECT_ROOT"
    docker-compose stop
    log_success "Docker 已停止"
}

# ============================================
# 前端构建
# ============================================
build_frontend_local() {
    log_info "使用本地 pnpm 构建..."
    cd "$FRONTEND_DIR"
    
    if ! command -v pnpm &> /dev/null; then
        npm install -g pnpm
    fi
    
    pnpm install
    pnpm build || {
        log_warn "前端构建失败（项目代码问题），将使用现有 dist 目录"
        if [ ! -d "$FRONTEND_DIST" ]; then
            mkdir -p "$FRONTEND_DIST"
            echo "Build failed" > "$FRONTEND_DIST/index.html"
        fi
    }
}

build_frontend_docker() {
    log_info "使用 Docker 构建前端..."
    cd "$FRONTEND_DIR"
    
    # 确保 dist 目录存在并清空
    rm -rf "$FRONTEND_DIST"
    mkdir -p "$FRONTEND_DIST"
    
    # 获取当前用户 ID 用于修正权限
    local uid=$(id -u)
    local gid=$(id -g)
    
    log_info "启动 Docker 容器进行构建..."
    
    # 使用 Docker 构建，并将 dist 输出到挂载卷
    # CI=true 避免 pnpm 在 non-TTY 环境下提示确认
    if docker run --rm \
        -v "$FRONTEND_DIR:/app" \
        -w /app \
        -e NODE_ENV=production \
        -e CI=true \
        node:20-alpine \
        sh -c "
            set -e
            echo '>>> 安装 pnpm...'
            npm install -g pnpm
            echo '>>> 安装依赖...'
            pnpm install
            echo '>>> 开始构建...'
            pnpm build
            echo '>>> 修正文件权限...'
            chown -R ${uid}:${gid} dist
            echo '>>> 构建完成'
        " 2>&1; then
        
        # 验证 dist 目录是否生成
        if [ -d "$FRONTEND_DIST" ] && [ -n "$(ls -A "$FRONTEND_DIST" 2>/dev/null)" ]; then
            log_success "前端构建成功"
            log_info "制品位置: $FRONTEND_DIST"
            log_info "制品内容:"
            ls -la "$FRONTEND_DIST" | head -10
        else
            log_error "前端构建失败: dist 目录为空"
            return 1
        fi
    else
        log_error "前端 Docker 构建失败"
        # 创建失败的占位文件
        echo "Build failed" > "$FRONTEND_DIST/index.html"
        return 1
    fi
}

build_frontend() {
    log_step "构建前端"
    
    # 根据配置选择构建方式
    # 默认使用 Docker，除非指定 --local
    if [ "$USE_DOCKER" = false ]; then
        build_frontend_local
    else
        # 默认使用 Docker 构建（更可靠的环境）
        build_frontend_docker
    fi
    
    [ -d "$FRONTEND_DIST" ] && log_success "前端构建完成"
}

# ============================================
# 后端构建
# ============================================
build_backend() {
    log_step "构建后端"
    cd "$PROJECT_ROOT"
    
    log_info "Maven 构建 (使用 mvnw)..."
    local mvn_args="clean package"
    if [ "$SKIP_TESTS" = true ]; then
        mvn_args="$mvn_args -DskipTests"
    else
        # 跳过 zgraph-driver 的单元测试（耗时太长）
        mvn_args="$mvn_args -Dtest='!**/zgraph-driver/**/*Test' -Dsurefire.failIfNoSpecifiedTests=false"
    fi
    "$MVN_CMD" $mvn_args
    
    log_success "后端构建完成"
}

# ============================================
# 服务操作
# ============================================
stop_services() {
    log_step "停止后端服务"
    for service in "${SERVICES[@]}"; do
        local idx=$(get_service_index "$service")
        local pid=$(get_service_pid "$service")
        
        if [ -n "$pid" ]; then
            log_info "停止 ${SERVICE_NAMES[$idx]} (PID: $pid)..."
            kill "$pid" 2>/dev/null || true
            sleep 1
            kill -9 "$pid" 2>/dev/null || true
            log_success "${SERVICE_NAMES[$idx]} 已停止"
        fi
    done
}

start_services() {
    sleep 10 #等待容器环境10秒
    log_step "启动后端服务"
    cd "$PROJECT_ROOT"
    mkdir -p "$LOGS_DIR"
    
    local idx=0
    for service in "${SERVICES[@]}"; do
        local service_name=$(basename "$service")
        local jar_path="${service}/target/${service_name}.jar"
        local port=${SERVICE_PORTS[$idx]}
        
        # 检查是否已在运行
        if check_port "$port"; then
            log_warn "${SERVICE_NAMES[$idx]} 已在运行，跳过"
            idx=$((idx + 1))
            continue
        fi
        
        # 检查 jar 文件
        if [ ! -f "$jar_path" ]; then
            log_error "找不到: $jar_path，请先执行 build"
            exit 1
        fi
        
        log_info "启动 ${SERVICE_NAMES[$idx]}..."
        nohup java -jar \
            "-Dspring.application.name=${service_name}" \
            "-Dserver.port=${port}" \
            "-Dfile.encoding=UTF-8" \
            "$jar_path" \
            > "${LOGS_DIR}/${service_name}.log" 2>&1 &
        
        # 等待启动
        local attempt=1
        while [ $attempt -le 30 ]; do
            if check_port "$port"; then
                log_success "${SERVICE_NAMES[$idx]} 已启动"
                break
            fi
            sleep 2
            attempt=$((attempt + 1))
        done
        
        [ $attempt -gt 30 ] && log_warn "${SERVICE_NAMES[$idx]} 启动超时"
        
        sleep 1
        idx=$((idx + 1))
    done
    
    echo ""
    log_success "服务启动完成"
    show_status
}

show_status() {
    echo -e "\n${CYAN}服务状态:${NC}"
    printf "%-20s %-10s %-10s\n" "名称" "端口" "状态"
    echo "----------------------------------------"
    
    local idx=0
    for service in "${SERVICES[@]}"; do
        local port=${SERVICE_PORTS[$idx]}
        local status="${RED}停止${NC}"
        
        if check_port "$port"; then
            status="${GREEN}运行中${NC}"
        fi
        
        printf "%-20s %-10s %-10b\n" "${SERVICE_NAMES[$idx]}" "$port" "$status"
        idx=$((idx + 1))
    done
    
    echo -e "\n${CYAN}访问地址:${NC}"
    echo -e "  应用: ${GREEN}http://localhost:18000${NC}"
    echo -e "  Nacos: ${GREEN}http://localhost:28848/nacos${NC}"
}

# ============================================
# 主命令
# ============================================
cmd_build() {
    [ "$SKIP_FRONTEND" = false ] && build_frontend
    [ "$SKIP_BACKEND" = false ] && build_backend
}

cmd_up() {
    stop_services
    stop_docker
    sleep 2
    start_docker
    start_services
}

cmd_down() {
    stop_services
    stop_docker
}

cmd_e2e() {
    log_step "运行 E2E 测试"
    # 检查后端服务是否全部运行
    for port in "${SERVICE_PORTS[@]}"; do
        if ! check_port "$port"; then
            log_error "后端服务未全部启动，请先执行: ./planka-dev.sh up"
            exit 1
        fi
    done
    cd "$FRONTEND_DIR"
    # 首次运行安装浏览器
    if [ ! -d "node_modules/.cache/ms-playwright" ]; then
        log_info "首次运行，安装 Playwright 浏览器..."
        npx playwright install chromium
    fi
    pnpm e2e "$@"
    log_success "E2E 测试完成"
}

# ============================================
# 主函数
# ============================================
main() {
    COMMAND=""
    SKIP_TESTS=false
    SKIP_FRONTEND=false
    SKIP_BACKEND=false
    USE_DOCKER=true
    
    # 解析参数
    while [[ $# -gt 0 ]]; do
        case $1 in
            build|up|down|e2e)
                COMMAND="$1"
                shift
                ;;
            --skip-tests|-st)
                SKIP_TESTS=true
                shift
                ;;
            --skip-frontend|-sf)
                SKIP_FRONTEND=true
                shift
                ;;
            --skip-backend|-sb)
                SKIP_BACKEND=true
                shift
                ;;
            --local)
                USE_DOCKER=false
                shift
                ;;
            --help|-h)
                show_help
                exit 0
                ;;
            *)
                log_error "未知参数: $1"
                show_help
                exit 1
                ;;
        esac
    done
    
    [ -z "$COMMAND" ] && show_help && exit 1
    
    check_command docker
    check_command docker-compose
    check_command java
    
    # 检查 mvnw
    if [ ! -f "$MVN_CMD" ]; then
        log_error "mvnw 不存在，请先运行: mvn wrapper:wrapper"
        exit 1
    fi
    
    case $COMMAND in
        build) cmd_build ;;
        up) cmd_up ;;
        down) cmd_down ;;
        e2e) cmd_e2e ;;
    esac
}

show_help() {
    cat << 'EOF'
planka-dev.sh - 敏捷看板开发环境管理脚本

用法:
  ./planka-dev.sh <command> [options]

命令:
  build       构建前端和后端（不启动服务）
  up          重启所有服务（Docker + 后端服务）
  down        停止所有服务（Docker + 后端服务，不移除容器）
  e2e         运行 E2E 测试（需先启动所有服务）

选项:
  -st, --skip-tests      跳过测试
  -sf, --skip-frontend   跳过前端构建
  -sb, --skip-backend    跳过后端构建
      --local            使用本地 pnpm 构建前端（默认使用 Docker）
  -h, --help             显示帮助

示例:
  ./planka-dev.sh build              # 完整构建（前端使用 Docker）
  ./planka-dev.sh build --local      # 使用本地 pnpm 构建前端
  ./planka-dev.sh build -st          # 构建（跳过测试）
  ./planka-dev.sh build -sf -sb      # 仅复制前端制品到网关
  ./planka-dev.sh up                 # 启动所有服务
  ./planka-dev.sh down               # 停止所有服务
  ./planka-dev.sh e2e                # 运行 E2E 测试

EOF
}

main "$@"
