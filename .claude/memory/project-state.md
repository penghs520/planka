# 项目状态

## 当前版本
- 主分支: main

## 最近变更
- 2024-03-16: 包结构重命名

## 已知问题
- 暂无

## 开发环境端口

| 服务 | 端口 |
|------|------|
| 前端开发服务器 | 13000 |
| API 网关 | 18000 |
| Card Service | 18101 |
| User Service | 18102 |
| View Service | 18103 |
| OSS Service | 18104 |
| History Service | 18105 |
| Comment Service | 18106 |
| Schema Service | 18100 |
| Nacos | 28848 |
| MySQL | 23306 |
| Redis | 26379 |
| Kafka | 29092 |

## 常用命令速查

```bash
# 构建
./scripts/planka-dev.sh build

# 启动所有服务
./scripts/planka-dev.sh up

# 停止所有服务
./scripts/planka-dev.sh down

# 前端开发
cd planka-ui && pnpm dev

# 后端编译
./mvnw clean compile

# 运行测试
./mvnw test
```
