# planka 开发规则

> 系统架构详见 [docs/系统整体架构设计.md](docs/系统整体架构设计.md)

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
- 前端: http://localhost:13000
- Nacos: http://localhost:28848/nacos (nacos/nacos)

---

## 项目结构

```
planka/
├── docs/                   # 功能设计文档（禁止过程文档）
├── planka-ui/              # 前端 (Vue 3 + Arco Design + UnoCSS)
├── planka-shared/          # 共享核心模块
├── planka-infra/           # 基础设施
├── planka-apis/            # API 定义
├── planka-services/        # 微服务模块
├── zgraph/                 # 图数据库 (Rust)
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

## 核心契约

### 沟通与检查
- 永远使用简体中文
- 代码（类、方法等）必须真实存在，不可捏造
- `docs/` 只存放功能设计文档，禁止过程文档

### 代码质量
- 单文件 < 600 行
- 不要实现 `Serializable`，使用 Jackson
- 避免使用 Object 类型，使用强类型 + Jackson 多态序列化
- Schema ID 使用 `SchemaId` 子类

### 数据库管理
- **禁止使用 Flyway** 等迁移工具
- 表结构统一在 `docker/mysql/init/` 初始化脚本中维护

### Git 规范
- **严禁使用 `--no-verify`** 跳过 Git Hooks
- 提交格式: `<type>: <description>`
- Type: `feat`, `fix`, `docs`, `style`, `refactor`, `test`, `chore`

---

## 命名规范

- 包名: `cn.planka.{module}`
- 类名后缀: `Id`, `Definition`, `Config`, `Entity`, `Event`, `DTO`, `Request`, `Response`, `Client`, `Service`, `Repository`, `Mapper`, `Converter`

---

## 详细规范

| 规范 | 位置 |
|------|------|
| 前端样式规范 | `.claude/rules/frontend-style.md` |
| 前端 API 规范 | `.claude/rules/frontend-api.md` |
| 后端架构规范 | `.claude/rules/backend-architecture.md` |
| Schema 缓存规范 | `.claude/rules/backend-schema-cache.md` |
| 测试规范 | `.claude/rules/testing.md` |
| 数据库规范 | `.claude/rules/database.md` |

## 工具文档

| 工具类型 | 位置 |
|----------|------|
| 前端通用组件 | `.claude/memory/frontend-components.md` |
| 后端通用工具 | `.claude/memory/backend-tools.md` |
| 项目状态 | `.claude/memory/project-state.md` |
