# planka 开发规则

> 系统架构详见 [docs/architecture（系统整体架构设计）.md](docs/architecture（系统整体架构设计）.md)


**访问地址**：
- 前端访问界面: http://localhost:3000 (`cd kanban-ui && pnpm dev`)
- Nacos: http://localhost:18848/nacos (nacos/nacos)

---

## 项目结构

```
planka/
├── docs/                   # 放置功能设计文档，禁止存放过程文档，如缺陷修复过程、优化过程等
├── kanban-ui/              # 前端 (Vue 3 + Arco Design + UnoCSS)
├── kanban-kernel/          # 核心领域模型
├── kanban-services/        # 微服务模块
├── kanban-apis/            # API 定义
├── kanban-infrastructure/  # 基础设施
├── kanban-starters/        # Spring Boot Starters
├── kanban-integration-test/# 集成测试
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

### 代码质量
- 单文件 < 600 行
- 不要实现 `Serializable`，使用Jackson的序列化
- 多类型场景避免使用Object类型，使用强类型，并考虑使用Jackson的多态序列化机制
- Schema ID 使用 `SchemaId` 子类

### 避免坏味道
- 避免僵化、冗余、循环依赖、脆弱性、晦涩性、数据泥团、过度设计

---

## 命名规范

- 包名: `dev.planka.{module}`
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
- **原子化 CSS**: UnoCSS（配置见 `kanban-ui/uno.config.ts`）
- **主题变量**: `kanban-ui/src/styles/theme.css`

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

## Git 提交规范

格式: `<type>: <description>`

Type: `feat`, `fix`, `docs`, `style`, `refactor`, `test`, `chore`
