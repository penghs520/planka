# agilean-planka 开发规则

> 系统架构详见 [docs/architecture（系统整体架构设计）.md](docs/architecture（系统整体架构设计）.md)

## Quick Start

```bash
# 构建前后端
./scripts/planka-dev.sh build

# 启动所有服务（Docker + 后端）
./scripts/planka-dev.sh up

# 停止所有服务
./scripts/planka-dev.sh down
```

**访问地址**：
- 前端访问界面: http://localhost:13000 (`cd planka-ui && pnpm dev`)
- Nacos: http://localhost:28848/nacos (nacos/nacos)

---

## 项目结构

```
agilean-planka2/
├── docs/                   # 放置功能设计文档，禁止存放过程文档，如缺陷修复过程、优化过程等
├── planka-ui/              # 前端 (Vue 3 + Arco Design + UnoCSS)
├── planka-kernel/          # 核心领域模型
├── planka-services/        # 微服务模块
├── planka-apis/            # API 定义
├── planka-infrastructure/  # 基础设施
├── planka-starters/        # Spring Boot Starters
├── planka-integration-test/# 集成测试
├── zgraph/                 # 图数据库
├── zgraph-driver/          # 图数据库驱动
├── docker/                 # Docker 配置
└── scripts/                # 开发脚本
```

---

## 环境依赖

- Java 17
- Maven 3.8+
- pnpm 10+
- Docker & Docker Compose
- Node.js 18+

---

## 核心规则

### 沟通与检查
- 永远使用简体中文
- 代码（类、方法等）必须真实存在，不可捏造
- 保持 `docs/` 文档与代码同步，`docs/`下禁止防止过程文档，如缺陷修复的文档，优化过程文档等，只存放最终的功能设计文档

### 数据库管理
- 禁止使用 Flyway 等数据库迁移工具，表结构统一在 `docker/mysql/init/` 初始化脚本中维护
- 新增或修改表结构时，直接更新对应的初始化 SQL 文件

### 代码质量
- 单文件 < 600 行
- 不要实现 `Serializable`，使用Jackson的序列化
- 多类型场景避免使用Object类型，使用强类型，并考虑使用Jackson的多态序列化机制
- Schema ID 使用 `SchemaId` 子类

### 避免坏味道
- 避免僵化、冗余、循环依赖、脆弱性、晦涩性、数据泥团、过度设计

### 文档与代码一致性
- 组件/工具的具体使用方式以代码为准，文档仅作快速索引
- 文档可能落后于代码，查看最新实现请直接阅读源码
- 添加新工具时，优先在代码中写清楚注释和示例

---

## 命名规范

- 包名: `cn.planka.{module}`
- 类名后缀参考：`Id`, `Definition`, `Config`, `Entity`, `Event`, `DTO`, `Request`, `Response`, `Client`, `Service`, `Repository`, `Mapper`, `Converter`

---

## 测试规范

**强制执行**：Git Hooks 会自动运行测试，失败禁止提交。**严禁使用 `--no-verify`**。

### 后端
- JUnit 5 + AssertJ
- Service 层 Mock Repository
- 测试类同包名

### 前端
- Vitest + Vue Test Utils
- 必须测试：工具函数 (utils)、通用组件 (components/common)、关键业务逻辑

---

## 前端样式规范（飞书风格）

### 技术栈
- **UI 框架**: Arco Design Vue
- **原子化 CSS**: UnoCSS（配置见 `planka-ui/uno.config.ts`）
- **主题变量**: `planka-ui/src/styles/theme.css`

### 配色
| 用途 | 变量 | 色值 |
|------|------|------|
| 主色 | `--color-primary` | `#3370FF` |
| 主色悬停 | `--color-primary-hover` | `#4E83FD` |
| 成功色 | `--color-success` | `#34C759` |
| 警告色 | `--color-warning` | `#FF9500` |
| 危险色 | `--color-danger` | `#F54A45` |
| 主文本 | `--color-text-1` | `#1F2329` |
| 次要文本 | `--color-text-2` | `#646A73` |
| 边框 | `--color-border-1` | `#E5E6EB` |

### 字体
```css
font-family: "PingFang SC", "HarmonyOS Sans SC", "Microsoft YaHei", -apple-system, sans-serif;
```

### 圆角
- 输入框/按钮: `6px` (`--radius-md`)
- 卡片/下拉: `8px` (`--radius-lg`)
- 弹窗: `12px` (`--radius-xl`)

### UnoCSS 使用
```html
<!-- 布局 -->
<div class="flex items-center gap-4 p-4">

<!-- 快捷方式 -->
<button class="btn-primary">保存</button>
<div class="card"><div class="card-body">内容</div></div>

<!-- 属性化模式 -->
<div flex items-center justify-between p="4">
```

常用快捷方式：`btn-primary`, `btn-secondary`, `btn-danger`, `card`, `card-header`, `card-body`, `flex-center`, `flex-between`

---

## 前端国际化 (i18n)

- **禁止硬编码中文**
- 语言文件：`src/i18n/locales/{zh-CN,en-US}/`
- Key 命名：`common.action.save`, `admin.cardType.title`
- 使用：
  - Vue: `{{ t('key') }}`
  - TS: `i18n.global.t('key')`
  - Arco Rule: `computed(() => [{ message: t('key') }])`

---

## 前端通用组件

| 组件 | 路径 | 适用场景                     | 详细文档 |
|------|------|--------------------------|----------|
| `CompactModal` | `src/utils/compactModal.ts` | 强制用户操作的模态框场景             | [查看详情](docs/dev/tools.md#compactmodal紧凑型模态框) |
| `TextExpressionTemplateEditor` | `src/components/common/text-expression-template/` | 通知模板、卡片标题模板、容器权限中的提示模板编辑 | [查看详情](docs/dev/tools.md#textexpressiontemplateeditor文本表达式模板编辑器) |
| `useLoading` | `src/hooks/useLoading.ts` | 异步操作加载状态管理               | [查看详情](docs/dev/tools.md#useloading加载状态管理) |
| `storage` | `src/utils/storage.ts` | localStorage 封装，用户偏好设置持久化 | [查看详情](docs/dev/tools.md#storage本地存储工具) |

---

## 后端通用工具

| 工具 | 路径 | 适用场景 | 详细文档 |
|------|------|----------|----------|
| `AssertUtils` | `planka-common/util/AssertUtils.java` | 参数校验、前置条件检查 | [查看详情](docs/dev/tools.md#assertutils---参数校验断言) |
| `ConditionYieldBuilder` | `card-api/util/ConditionYieldBuilder.java` | 从条件中提取字段构建 Yield | [查看详情](docs/dev/tools.md#conditionyieldbuilder---条件-yield-构建器) |
| `TextExpressionTemplateResolver` | `planka-infra-expression/TextExpressionTemplateResolver.java` | 解析模板变量生成动态文本 | [查看详情](docs/dev/tools.md#textexpressiontemplateresolver---模板表达式解析器) |
| `CardCacheService` | `planka-infra-card-cache/` | 卡片基础信息二级缓存 | [查看详情](docs/dev/tools.md#cardcacheservice---卡片缓存) |
| `SchemaCacheService` | `planka-infra-schema-cache/` | Schema 定义二级缓存 | [查看详情](docs/dev/tools.md#schemacacheservice---schema-缓存) |
| `FieldConfigQueryService` | `schema-api/service/FieldConfigQueryService.java` | 获取卡片类型字段配置（含继承解析） | [查看详情](docs/dev/tools.md#fieldconfigqueryservice---字段配置查询) |

### 具体类型的Schema 缓存查询类

| 查询类 | 适用场景 |
|--------|----------|
| `CardTypeCacheQuery` | 卡片类型缓存查询，支持按组织ID查询 |
| `ViewCacheQuery` | 视图定义缓存查询 |
| `CardFaceCacheQuery` | 卡片面版模板缓存查询，支持按卡片类型查询 |
| `BizRuleCacheQuery` | 业务规则缓存查询，支持按卡片类型查询 |
| `LinkTypeCacheQuery` | 关联类型缓存查询，支持按卡片类型查询 |
| `ValueStreamCacheQuery` | 价值流缓存查询 |
| `CardActionCacheQuery` | 卡片动作缓存查询 |
| `CardPermissionCacheQuery` | 卡片权限缓存查询 |

## 更多通用工具的使用
请查阅docs/dev/tools.md文档以及代码库，以代码库为准
---

## 工具使用文档的持续更新
当组件新增或发生变化时应主动更新对应的文档

## Git 提交规范

格式: `<type>: <description>`

Type: `feat`, `fix`, `docs`, `style`, `refactor`, `test`, `chore`
