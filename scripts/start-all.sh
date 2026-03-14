#!/bin/bash
# 启动所有服务（Docker + 后端）
cd "$(dirname "$0")" && ./kanban-dev.sh up
