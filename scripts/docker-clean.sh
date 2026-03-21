#!/bin/bash

# 停止所有容器并清除数据卷
# 警告：此操作会删除所有容器数据，请谨慎使用！

set -e

echo "🛑 停止所有容器..."
docker-compose down

echo ""
echo "🗑️  移除所有容器和数据卷..."
docker-compose down -v

echo ""
echo "✅ 清理完成！所有容器和数据卷已删除"
echo ""
echo "如需重新启动，请运行:"
echo "  docker-compose up -d"
