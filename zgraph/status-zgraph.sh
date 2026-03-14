#!/bin/bash
# zgraph 状态检查脚本

echo "📊 zgraph 状态检查"
echo "===================="

# 检查进程
if ps aux | grep "[z]graph" > /dev/null; then
    echo "✅ 进程状态: 运行中"
    ps aux | grep "[z]graph"
else
    echo "❌ 进程状态: 未运行"
fi

echo ""

# 检查端口
if lsof -i :3897 > /dev/null 2>&1; then
    echo "✅ 端口状态: 3897 已监听"
    lsof -i :3897
else
    echo "❌ 端口状态: 3897 未监听"
fi

echo ""

# 显示最近日志
if [ -f /tmp/zgraph/logs/zgraph.log ]; then
    echo "📝 最近日志 (最后 10 行):"
    echo "===================="
    tail -10 /tmp/zgraph/logs/zgraph.log
else
    echo "⚠️  日志文件不存在"
fi
