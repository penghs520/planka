#!/bin/bash
# zgraph 启动脚本

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$SCRIPT_DIR"

# 检查是否已经在运行
if lsof -i :13897 > /dev/null 2>&1; then
    echo "✅ zgraph 已在运行"
    ps aux | grep "[z]graph"
    exit 0
fi

# 确保日志目录存在
mkdir -p /tmp/zgraph/logs

# 启动 zgraph
echo "🚀 启动 zgraph..."
nohup ./target/release/zgraph -c zgraph.conf > /tmp/zgraph/logs/zgraph.log 2>&1 &

# 等待启动
sleep 3

# 检查是否启动成功
if lsof -i :13897 > /dev/null 2>&1; then
    echo "✅ zgraph 启动成功"
    echo "📍 监听地址: 127.0.0.1:13897"
    echo "📝 日志文件: /tmp/zgraph/logs/zgraph.log"
    ps aux | grep "[z]graph"
else
    echo "❌ zgraph 启动失败，查看日志:"
    tail -20 /tmp/zgraph/logs/zgraph.log
    exit 1
fi
