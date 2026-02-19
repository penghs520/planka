# AGENTS.md - AI 编码代理快速参考指南

本文件为 AI 编码代理提供快速参考，详细架构设计请参考 [docs/architecture（系统整体架构设计）.md](docs/architecture（系统整体架构设计）.md)。

---

## 1. 项目概述

planka 是一个企业级敏捷研发管理平台，采用微服务架构和零代码设计理念，支持灵活的数据建模、可视化关联管理和多维度视图展示。

### 核心功能
- **灵活的数据建模**：自定义卡片类型、属性定义、类型继承
- **可视化关联管理**：卡片类型间的关联关系定义与 ER 图展示
- **多维度视图**：列表视图、看板视图、自定义筛选条件
- **价值流管理**：支持价值流状态定义与流转

---

## 2. 技术栈

### 后端
| 技术 | 版本 | 说明 |
|------|------|------|
| Java | 17+ | 运行时环境（kanban-apis 使用 Java 21） |
| Spring Boot | 3.4.6 | 应用框架 |
| Spring Cloud | 2024.0.1 | 微服务框架 |
| Spring Cloud Alibaba | 2023.0.3.3 | 微服务组件（Nacos） |
| MyBatis Plus | 3.5.10.1 | ORM 框架 |
| MySQL | 8.0+ | 关系型数据存储（支持达梦数据库） |
| Kafka | 3.9.0 | 事件总线 |
| Redis | 7.0 | 缓存 |
| Protobuf | 4.28.2 | 与 zgraph 通信协议 |

### 前端
| 技术 | 版本 | 说明 |
|------|------|------|
| Vue | 3.5.25 | 前端框架 |
| TypeScript | 5.9.3 | 类型系统 |
| Vite | 7.2.6 | 构建工具 |
| Pinia | 3.0.4 | 状态管理 |
| Arco Design Vue | 2.57.0 | UI 组件库 |
| Vue Router | 4.6.3 | 路由管理 |
| Vue Flow | 1.48.0 | 流程图/ER图 |
| vue-i18n | 11.2.7 | 国际化 |
| Vitest | 4.0.18 | 测试框架 |

### 图数据库
- **zgraph**：Rust 实现的图数据库，作为卡片的存储查询引擎
- **zgraph-driver**：Java 驱动，通过 Protobuf 协议与 zgraph 通信

---

## 3. 项目结构

```
planka/
├── pom.xml                         # 根 POM，依赖版本管理
│
├── kanban-kernel/                  # 微内核核心
│   ├── kanban-common/              # 公共工具、常量、异常、Result
│   ├── kanban-domain/              # 领域模型：Schema定义、Card、Field、Link等
│   └── kanban-event/               # 领域事件定义
│
├── kanban-infrastructure/          # 公共基础设施
│   ├── kanban-infra-schema-cache/  # Schema 缓存实现
│   └── kanban-infra-card-cache/    # 卡片数据缓存实现
│
├── kanban-apis/                    # 服务 API Client（OpenFeign）
│   ├── schema-api/                 # Schema 服务 API
│   ├── user-api/                   # User 服务 API
│   ├── card-api/                   # Card 服务 API
│   └── view-api/                   # View 服务 API
│
├── kanban-services/                # 应用服务
│   ├── schema-service/             # Schema 定义服务
│   ├── card-service/               # 卡片数据服务
│   ├── view-service/               # 视图数据服务
│   ├── user-service/               # 用户认证服务
│   ├── gateway-service/            # API 网关
│   ├── extension-service/          # 扩展服务（操作历史等）
│   ├── ai-assistant-service/       # AI 助手服务
│   └── notification/               # 通知服务
│       ├── notification-service/
│       └── notification-plugins/
│           ├── notification-plugin-api/
│           └── notification-plugin-dingding/
│
├── kanban-starters/                # Spring Boot Starters（预留）
├── kanban-integration-test/        # 集成测试
│
├── zgraph-driver/                  # 图数据库 Java 驱动
├── zgraph/                         # 图数据库（Rust 实现）
├── proto/                          # Protobuf 协议定义
│
├── kanban-ui/                      # 前端应用（Vue 3 + TypeScript）
│   ├── src/
│   │   ├── api/                    # API 接口封装
│   │   ├── components/             # 公共组件
│   │   ├── composables/            # Composition API 复用逻辑
│   │   ├── hooks/                  # Vue Hooks
│   │   ├── i18n/                   # 国际化（zh-CN, en-US）
│   │   ├── layouts/                # 布局组件
│   │   ├── router/                 # 路由配置
│   │   ├── stores/                 # Pinia 状态管理
│   │   ├── styles/                 # 全局样式
│   │   ├── types/                  # TypeScript 类型定义
│   │   ├── utils/                  # 工具函数
│   │   └── views/                  # 页面视图
│   ├── package.json
│   ├── vite.config.ts
│   └── vitest.config.ts
│
├── docker-compose.yml              # 本地开发环境（Nacos/MySQL/Redis/Kafka）
├── .githooks/                      # Git Hooks
└── docs/                           # 项目文档
```

---

## 4. 构建和测试命令

### 后端（Maven）

```bash
# 编译项目
mvn clean compile

# 运行所有测试
mvn test

# 运行指定模块测试
mvn test -pl kanban-services/card-service -am

# 打包
mvn clean package

# 安装到本地仓库
mvn clean install

# 跳过测试打包
mvn clean package -DskipTests
```

### 前端（pnpm）

```bash
cd kanban-ui

# 安装依赖
pnpm install

# 启动开发服务器
pnpm dev

# 构建生产版本
pnpm build

# 运行单元测试
pnpm test:run

# 运行测试（监听模式）
pnpm test

# TypeScript 类型检查
npx vue-tsc --noEmit
```

### 本地开发环境

```bash
# 启动基础设施服务（Nacos、MySQL、Redis、Kafka）
docker-compose up -d

# 查看服务状态
docker-compose ps

# 停止服务
docker-compose down
```

### 服务端口

| 服务 | 端口 |
|------|------|
| Nacos | 18848 (HTTP), 19848 (gRPC) |
| MySQL | 13306 |
| Redis | 16379 |
| Kafka | 19092 |
| 前端开发服务器 | 3000 |
| 网关服务 | 8000 |

---

## 5. 代码规范

### 命名规范

- **包名**: `dev.planka.{module}`
- **类名后缀规范**:
  - `Id` - 标识符类（如 `CardId`, `SchemaId`）
  - `Definition` - 定义类
  - `Config` - 配置类
  - `Entity` - 数据库实体
  - `Event` - 领域事件
  - `DTO` - 数据传输对象
  - `Request/Response` - API 请求/响应
  - `Client` - Feign 客户端
  - `Service` - 业务服务
  - `Repository` - 数据访问
  - `Mapper` - MyBatis 映射器
  - `Converter` - 类型转换器

### 代码质量要求

- **单文件限制**: 小于 600 行
- **序列化**: 使用 Jackson，**禁止**实现 `Serializable`
- **类型安全**: 多类型场景避免使用 `Object`，使用强类型，考虑 Jackson 多态序列化
- **Schema ID**: 使用 `SchemaId` 子类，避免直接使用字符串

### 禁止的坏味道

- 僵化、冗余、循环依赖
- 脆弱性、晦涩性
- 数据泥团、过度设计

---

## 6. 测试规范

**⚠️ 强制执行**: Git Hooks 会自动运行测试，失败禁止提交。**严禁使用 `--no-verify`**。

### 后端测试

- **框架**: JUnit 5 + AssertJ + Mockito
- **Service 层**: Mock Repository 进行单元测试
- **测试类位置**: 与被测试类同包名，放在 `src/test/java`

```java
@ExtendWith(MockitoExtension.class)
class SomeServiceTest {
    @Mock
    private SomeRepository repository;
    
    @InjectMocks
    private SomeService service;
    
    @Test
    void shouldDoSomething() {
        // given
        given(repository.findById(any())).willReturn(Optional.of(entity));
        
        // when
        Result result = service.doSomething();
        
        // then
        assertThat(result).isNotNull();
    }
}
```

### 前端测试

- **框架**: Vitest + Vue Test Utils + jsdom
- **必须测试**: 工具函数 (utils)、通用组件 (components/common)、关键业务逻辑

```typescript
import { describe, it, expect } from 'vitest'
import { mount } from '@vue/test-utils'
import Component from './Component.vue'

describe('Component', () => {
  it('should render correctly', () => {
    const wrapper = mount(Component)
    expect(wrapper.text()).toContain('expected')
  })
})
```

---

## 7. 前端国际化 (i18n)

**⚠️ 禁止硬编码中文**

- **语言文件位置**: `src/i18n/locales/{zh-CN,en-US}/`
- **Key 命名规范**: `common.action.save`, `admin.cardType.title`

### 使用方式

```vue
<!-- Vue 模板 -->
<template>
  <button>{{ t('common.action.save') }}</button>
</template>

<script setup>
import { useI18n } from 'vue-i18n'
const { t } = useI18n()
</script>
```

```typescript
// TypeScript
import i18n from '@/i18n'
i18n.global.t('key')

// Arco Design 表单验证
import { computed } from 'vue'
const rules = computed(() => [
  { message: t('validation.required') }
])
```

---

## 8. Git 提交规范

### 提交格式

```
<type>: <description>
```

### Type 类型

- `feat`: 新功能
- `fix`: 修复
- `docs`: 文档
- `style`: 格式（不影响代码运行的变动）
- `refactor`: 重构
- `test`: 测试
- `chore`: 构建过程或辅助工具的变动

### 示例

```
feat: 添加卡片类型继承功能
fix: 修复视图筛选条件失效问题
docs: 更新 API 文档
```

---

## 9. 核心设计原则

### 三层架构

每个服务内部采用三层架构：`Controller → Service → Repository → Mapper`

- 业务逻辑集中在 Service 层
- 实体模型为贫血模型（仅包含数据，无业务行为）
- Repository 作为防腐层隔离数据访问细节

### Schema 定义体系

- **FieldDefinition**: 属性定义（组织级）
- **FieldConfig**: 属性配置（挂载到卡片类型时产生，可针对类型差异化配置）
- **CardType**: 卡片类型（属性集 / 实体类型）
- **LinkType**: 关联类型（定义卡片间关系）

### 缓存使用规范

- 除 `schema-service` 外的模块，**必须使用缓存**查询 Schema
- **禁止**使用 FeignClient 进行远程调用获取 Schema
- 使用 `kanban-infra-schema-cache` 模块提供的缓存能力

---

## 10. 开发环境配置

### 设置 Git Hooks

```bash
# 配置 Git 使用项目提供的 hooks
git config core.hooksPath .githooks

# 或者运行 setup 脚本
chmod +x .githooks/setup-hooks.sh
./.githooks/setup-hooks.sh
```

### 本地开发流程

1. 启动基础设施: `docker-compose up -d`
2. 启动后端服务（从 gateway-service 开始）
3. 启动前端: `cd kanban-ui && pnpm dev`
4. 访问: http://localhost:3000

---

## 11. 国产化支持

项目支持国产化环境，通过 Maven Profile 切换：

```bash
# 东方通 TongWeb 8.x
mvn clean package -Ptongweb-8.x

# 达梦数据库
mvn clean package -Pdameng

# 组合使用
mvn clean package -Ptongweb-8.x,dameng
```

---

## 12. 参考文档

- [系统整体架构设计](docs/architecture（系统整体架构设计）.md)
- [业务规则功能设计](docs/biz-rule（业务规则功能设计）.md)
- [卡片动作功能设计](docs/card-action（卡片动作功能设计）.md)
- [卡片类型继承设计](docs/card-type-inheritance（卡片类型继承设计）.md)
