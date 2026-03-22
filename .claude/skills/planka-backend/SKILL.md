---
name: planka-backend
description: 为 planka 项目开发后端功能。使用 Spring Cloud 微服务架构，Java 17。当用户要求开发后端 API、服务或领域模型时激活。
---

# planka 后端开发 Skill

## 技术栈
- Java 17+
- Spring Boot 3.4+
- Spring Cloud 2024.0.1
- MyBatis Plus 3.5+
- MySQL 8.0+
- Kafka（事件总线）

## 模块结构
```
planka-services/
├── gateway-service/      # API 网关 (18000)
├── schema-service/       # Schema 定义服务 (18107)
├── card-service/         # 卡片数据服务 (18100)
├── view-service/         # 视图数据服务 (18102)
├── user-service/         # 用户认证服务 (18101)
├── history-service/      # 操作历史 (18105)
├── comment-service/      # 评论 (18106)
└── oss/                  # 对象存储服务 (18103)
```

## 开发流程

### 1. 确定服务归属
- Schema 相关 → schema-service
- 卡片 CRUD → card-service
- 视图数据 → view-service
- 用户认证 → user-service

### 2. 代码规范
- 包名：`cn.planka.{module}`
- 类名后缀：`Id`, `Definition`, `Config`, `Entity`, `Event`, `DTO`, `Request`, `Response`, `Client`, `Service`, `Repository`, `Mapper`, `Converter`
- 单文件 < 600 行
- **不要实现 Serializable**，使用 Jackson
- 避免使用 Object 类型，使用强类型 + Jackson 多态序列化
- Schema ID 使用 `SchemaId` 子类

### 3. Schema 缓存（重要）
- schema-service 之外的服务必须使用缓存
- 禁止直接 FeignClient 调用查询 Schema
- 使用具体查询类：CardTypeCacheQuery, ViewCacheQuery 等

### 4. 数据库
- **禁止使用 Flyway**
- 表结构在 `docker/mysql/init/` 维护
- 新增/修改表结构时更新对应 SQL 文件

### 5. 测试
- JUnit 5 + AssertJ
- Service 层 Mock Repository
- 测试类与被测试类同包名

## 常用工具
- `AssertUtils` - 参数校验断言
- `ConditionYieldBuilder` - 条件 Yield 构建
- `TextExpressionTemplateResolver` - 模板表达式解析
- `CardCacheService` - 卡片缓存
- `SchemaCacheService` - Schema 缓存
