#!/bin/bash
# zgraph 停止脚本

echo "🛑 停止 zgraph..."

# 查找并停止 zgraph 进程
if pkill -9 zgraph; then
    echo "✅ zgraph 已停止"
else
    echo "ℹ️  没有运行中的 zgraph 进程"
fi

# 等待进程完全停止
sleep 1

# 验证是否已停止
if lsof -i :3897 > /dev/null 2>&1; then
    echo "⚠️  端口 3897 仍被占用"
else
    echo "✅ 端口 3897 已释放"
fi
