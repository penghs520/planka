#!/bin/bash
# 停止所有服务（Docker + 后端）
cd "$(dirname "$0")" && ./kanban-dev.sh down
