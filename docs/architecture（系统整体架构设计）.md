# planka 系统架构

## 项目概述

planka 是一个企业级敏捷研发管理平台，采用零代码架构设计，支持：

- **灵活的数据建模**：自定义卡片类型、属性定义、类型继承
- **可视化关联管理**：卡片类型间的关联关系定义与 ER 图展示
- **多维度视图**：列表视图、看板视图、自定义筛选条件
- **价值流管理**：支持价值流状态定义与流转

---

## 技术栈

### 后端

| 技术 | 版本 | 说明 |
|------|------|------|
| Java | 17+ | 运行时环境 |
| Spring Boot | 3.4+ | 应用框架 |
| Spring Cloud | 2024.0.1 | 微服务框架 |
| Spring Cloud Alibaba | 2023.0.3.3 | 微服务组件（Nacos） |
| MyBatis Plus | 3.5+ | ORM 框架 |
| MySQL | 8.0+ | 关系型数据存储 |
| zgraph | - | 图数据库（卡片存储） |
| gRPC/Protobuf | - | 与 zgraph 通信协议 |
| Kafka | - | 事件总线 |

### 前端

| 技术 | 版本 | 说明 |
|------|------|------|
| Vue | 3.5 | 前端框架 |
| TypeScript | 5.x | 类型系统 |
| Vite | 7 | 构建工具 |
| Pinia | 3.x | 状态管理 |
| Arco Design Vue | 2.x | UI 组件库 |
| Vue Router | 4 | 路由管理 |
| Vue Flow | - | 流程图/ER图 |
| vue-i18n | 11.x | 国际化 |

### 图数据库

- **zgraph**：Rust 实现的图数据库，作为卡片的存储查询引擎
- **zgraph-driver**：Java 驱动，通过 gRPC/Protobuf 协议与 zgraph 通信

---

## 模块结构

```
planka/
├── kanban-kernel/                  # 微内核核心
│   ├── kanban-common/              # 公共工具、常量、异常、Result
│   ├── kanban-domain/              # 领域模型：Schema定义、Card、Field、Link等
│   └── kanban-event/               # 领域事件定义
│
├── kanban-infrastructure/          # 公共基础设施
│   ├── kanban-infra-schema-cache/  # Schema 缓存实现，除了schema-service服务之外的模块，要求使用缓存进行查询schema，避免使用FeignClient进行远程调用
│   └── kanban-infra-card-cache/    # 卡片数据（简单卡片信息）缓存实现
│
├── kanban-apis/                    # 服务 API Client（OpenFeign）
│   ├── schema-api/                 # Schema 服务 API
│   ├── card-api/                   # Card 服务 API
│
├── kanban-services/                # 应用服务
│   ├── schema-service/             # Schema 定义服务
│   ├── card-service/               # 卡片数据服务
│   ├── view-service/               # 视图数据服务
│   ├── user-service/               # 用户认证服务
│   ├── gateway-service/            # API 网关
│   └── extension-service/          # 扩展服务（操作历史等）
│
├── kanban-starters/                # Spring Boot Starters
├── kanban-integration-test/        # 集成测试
│
├── zgraph-driver/                  # 图数据库 Java 驱动
├── zgraph/                         # 图数据库（Rust 实现）
├── proto/                          # Protobuf 协议定义
│
├── kanban-ui/                      # 前端应用（Vue 3 + TypeScript）
└── docs/                           # 项目文档
```

---

## 服务说明

### schema-service（Schema 定义服务）

元数据管理服务，负责所有配置定义的 CRUD 和生命周期管理：

- **属性定义**：文本、数字、日期、枚举、架构、附件等多种类型
- **卡片类型**：属性集、实体类型、继承关系
- **关联类型**：卡片类型间的关联关系定义
- **视图配置**：列表视图、看板视图的定义
- **菜单配置**：导航菜单结构
- **详情模板**：卡片详情页布局配置
- **价值流定义**：状态流转配置

### card-service（卡片数据服务）

卡片数据管理服务，通过 zgraph-driver 与图数据库交互：

- **卡片 CRUD**：创建、查询、打开详情页、更新（卡片和关联）、移动卡片状态、丢弃、归档、还原以及对应的批量操作等
- **条件查询**：支持复杂条件、分页、排序
- **权限控制**：创建、更新与查询等操作的灵活权限控制


### view-service（视图数据服务）

视图数据聚合服务，负责视图数据的查询和转换：

- **视图执行**：根据视图定义执行数据查询
- **条件合并**：合并视图条件与用户筛选条件
- **数据转换**：将查询结果转换为视图所需格式

### user-service（用户认证服务）

用户和组织管理服务：

- **用户管理**：注册、激活、个人信息
- **认证服务**：登录、JWT 令牌、刷新令牌
- **组织管理**：组织创建、设置
- **成员管理**：成员添加、角色变更、移除

### gateway-service（API 网关）

统一 API 入口：

- **路由转发**：将请求路由到对应微服务
- **JWT 认证**：验证请求令牌
- **CORS 配置**：跨域请求处理

### extension-service（扩展服务）

扩展功能模块，包含操作历史记录等功能：

- **操作历史**：记录卡片的所有变更操作，支持多维度查询

---

## 前端模块结构（kanban-ui）

```
kanban-ui/src/
├── api/              # API 接口封装
├── components/       # 公共组件
├── composables/      # Composition API 复用逻辑
├── hooks/            # Vue Hooks
├── i18n/             # 国际化（zh-CN, en-US）
├── layouts/          # 布局组件
├── router/           # 路由配置
├── stores/           # Pinia 状态管理
├── styles/           # 全局样式
├── types/            # TypeScript 类型定义
├── utils/            # 工具函数
└── views/            # 页面视图
    ├── auth/                 # 认证页面
    ├── org/                  # 组织管理
    ├── user/                 # 用户设置
    ├── workspace/            # 工作空间
    └── schema-definition/    # Schema 定义管理
```

---

## 核心设计原则

### 1. 简单三层架构

每个服务内部采用三层架构：Controller → Service → Repository → Mapper

- 业务逻辑集中在 Service 层
- 实体模型为贫血模型（仅包含数据，无业务行为）
- Repository 作为防腐层隔离数据访问细节

### 2. Schema 定义体系

- **FieldDefinition**：属性定义（组织级）
- **FieldConfig**：属性配置（挂载到卡片类型时产生，可针对类型差异化配置）
- **CardType**：卡片类型（属性集 / 实体类型）
- **LinkType**：关联类型（定义卡片间关系）

### 3. 统一关联标识

- 使用 `LinkTypeId + LinkPosition` 组合：`LinkTypeId:SOURCE|TARGET`

### 4. 类型安全的属性值

- TextFieldValue、NumberFieldValue、DateFieldValue
- EnumFieldValue、LinkFieldValue、AttachmentFieldValue 等
