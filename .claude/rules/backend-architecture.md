# 后端架构规范

## 模块结构

```
planka-services/
├── gateway-service/      # API 网关 (18000)
├── schema-service/       # Schema 定义服务 (18107)
├── card-service/         # 卡片数据服务 (18100)
├── view-service/         # 视图数据服务 (18102)
├── user-service/         # 用户认证服务 (18101)
├── extension-service/    # 扩展服务 (18104)
└── oss/                  # 对象存储服务 (18103)
```

## 分层规范

```
├── controller/           # 控制器层：参数校验、路由映射
├── service/              # 服务层：业务逻辑
├── repository/           # 仓储层：数据访问
├── mapper/               # MyBatis Mapper
├── entity/               # 数据库实体
├── dto/                  # 数据传输对象
├── converter/            # 类型转换器
└── event/                # 领域事件
```

## 代码规范

- 包名: `cn.planka.{module}`
- 类名后缀：
  - `Id` - 标识符
  - `Definition` - 定义类
  - `Config` - 配置类
  - `Entity` - 数据库实体
  - `Event` - 领域事件
  - `DTO` - 数据传输对象
  - `Request`/`Response` - API 请求/响应
  - `Client` - Feign 客户端
  - `Service` - 服务层
  - `Repository` - 仓储层
  - `Mapper` - MyBatis Mapper
  - `Converter` - 转换器

## 重要约束

1. **非 schema-service 必须使用缓存查询 Schema**，禁止直接 FeignClient 调用
2. **禁止实现 Serializable**，使用 Jackson 序列化
3. **避免使用 Object 类型**，使用强类型 + Jackson 多态序列化
4. **Schema ID 必须使用 `SchemaId` 子类**
