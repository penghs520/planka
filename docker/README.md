# Docker 基础设施环境

## 服务组件

| 服务 | 容器端口  | 宿主机端口 | 用途 |
|------|-------|-------|------|
| MySQL | 3306  | 23306 | 数据库 |
| Redis | 6379  | 26379 | 缓存 |
| Kafka | 29092 | 29092 | 消息队列 |
| Zookeeper | 2181  | 22181 | Kafka 依赖 |

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
- Port: 23306
- Database: planka_schema
- Username: root
- Password: root

## 验证服务

```bash
# 验证 MySQL
mysql -h localhost -P 23306 -u root -proot -e "SHOW DATABASES;"

# 验证 Redis
redis-cli -p 26379 ping

# 验证 Kafka
docker exec planka-kafka kafka-topics --bootstrap-server localhost:9092 --list
```

## 运行集成测试

```bash
# 1. 启动基础设施
docker-compose up -d

# 2. 等待服务就绪（约30秒）

# 3. 启动 schema-service（使用 docker profile）
mvn spring-boot:run -pl planka-services/schema-service -Dspring-boot.run.profiles=docker

# 4. 运行集成测试（新终端）
mvn test -pl planka-integration-test -Pintegration-test
```
