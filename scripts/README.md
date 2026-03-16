# 敏捷看板开发环境管理脚本（简化版）

## 简介

一键管理敏捷看板开发环境的脚本工具，只保留三个核心命令。

## 脚本文件

```
scripts/
├── planka-dev.sh      # 主脚本 (Linux/Mac)
├── planka-dev.bat     # 主脚本 (Windows)
├── build.sh           # 构建前后端
├── start-all.sh       # 快捷启动所有后端服务 + docker
└── stop-all.sh        # 快捷停止所有后端服务 + docker
```

## 命令

| 命令 | 说明 |
|------|------|
| `build` | 构建前端和后端（不启动服务） |
| `up` | 重启所有服务（Docker + 后端服务） |
| `down` | 停止所有服务（Docker + 后端服务，不移除容器） |

## 使用方法

### Linux/Mac

```bash
cd scripts

# 构建
./planka-dev.sh build

# 构建（跳过测试）
./planka-dev.sh build -st

# 启动所有服务
./planka-dev.sh up
# 或快捷方式
./start-all.sh

# 停止所有服务
./planka-dev.sh down
# 或快捷方式
./stop-all.sh
```

## 选项

| 选项 | 说明 |
|------|------|
| `-st, --skip-tests` | 跳过测试 |
| `-sf, --skip-frontend` | 跳过前端构建 |
| `-sb, --skip-backend` | 跳过后端构建 |

## 流程说明

### build 命令
```
1. 构建前端 (pnpm install && pnpm build)
3. Maven 打包所有服务
```

### up 命令
```
1. 停止所有后端服务
2. 停止 Docker 容器
3. 启动 Docker 容器 (Nacos, MySQL, Redis, Kafka)
4. 按顺序启动后端服务 (端口 8000, 8100-8107)
5. 显示服务状态
```

### down 命令
```
1. 停止所有后端服务
2. 停止 Docker 容器（不移除）
```

## 访问地址

- **应用**: http://localhost:8000
- **Nacos**: http://localhost:18848/nacos (nacos/nacos)
