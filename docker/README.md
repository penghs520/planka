# Docker 基础设施环境

## 服务组件

| 服务 | 容器端口  | 宿主机端口 | 用途 |
|------|-------|-------|------|
| MySQL | 3306  | 13306 | 数据库 |
| Redis | 6379  | 16379 | 缓存 |
| Kafka | 19092 | 19092 | 消息队列 |
| Zookeeper | 2181  | 12181 | Kafka 依赖 |

## 快速启动

```bash
# 在项目根目录执行
docker-compose up -d

# 查看服务状态
docker-compose ps

# 查看日志
docker-compose logs -f

# 停止服务
docker-compose down

# 停止并清除数据
docker-compose down -v
```

## MySQL 连接信息

- Host: localhost
- Port: 13306
- Database: kanban_schema
- Username: root
- Password: root

## 验证服务

```bash
# 验证 MySQL
mysql -h localhost -P 13306 -u root -proot -e "SHOW DATABASES;"

# 验证 Redis
redis-cli -p 16379 ping

# 验证 Kafka
docker exec kanban-kafka kafka-topics --bootstrap-server localhost:9092 --list
```

## 运行集成测试

```bash
# 1. 启动基础设施
docker-compose up -d

# 2. 等待服务就绪（约30秒）

# 3. 启动 schema-service（使用 docker profile）
mvn spring-boot:run -pl kanban-services/schema-service -Dspring-boot.run.profiles=docker

# 4. 运行集成测试（新终端）
mvn test -pl kanban-integration-test -Pintegration-test
```
