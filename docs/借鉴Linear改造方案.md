# 借鉴 Linear 改造方案

## 概述

在保持 planka 零代码核心优势的基础上，借鉴 Linear 的交互设计，从四个维度提升操作效率和视觉体验：界面风格、命令面板、多团队模式、Inbox 收件箱。

## 关键决策

1. **统一为一套布局** — 管理后台嵌入主布局侧边栏，不再需要独立的 DefaultLayout
2. **侧边栏支持深色/浅色切换** — Rosé Pine 配色方案，默认深色
3. **Team / Project / Issue 为内置__PLANKA_EINST__** — 复用 Schema + 图存储，不建 `sys_team` 表

## 实施顺序

```
Phase 1: 界面风格改造 ← 已完成主体，有遗留问题
Phase 2: 命令面板 Cmd+K
Phase 3: 多团队模式
Phase 4: Inbox 收件箱
```

---

## Phase 1: 界面风格改造（已完成主体）

### 已完成

- [x] 统一为单布局 `AppLayout.vue`，admin 路由嵌入同一侧边栏
- [x] 侧边栏组件拆分：SidebarHeader / SidebarQuickActions / SidebarAdmin / SidebarFooter
- [x] 深色/浅色主题切换（Rosé Pine 配色）
- [x] 主题偏好持久化 localStorage
- [x] 扁平化管理菜单（移除多层嵌套）
- [x] 组织图标改为渐变色块 + 首字母
- [x] 侧边栏宽度可拖拽调整（200-360px）
- [x] 角色加载修复（AppLayout onMounted 调用 fetchCurrentOrgRole）
- [x] i18n 支持（zh-CN / en-US sidebar 语言包）

### 遗留问题

- [ ] **主题切换按钮图标不可见** — `IconSun`/`IconMoon` 的 SVG width 为 0px，需要排查 Arco Design icon 在 `<button>` 内的渲染问题。可能需要用 `<span>` 包裹或改用 Arco 的 `<a-button>` 组件
- [ ] 组织图标渐变色需要同步 Rosé Pine 配色（当前还是紫色渐变）

### 配色方案

**浅色主题 (Rosé Pine Dawn)**：
```css
--sidebar-bg: #FFFAF3;
--sidebar-bg-hover: #FAF4ED;
--sidebar-bg-active: #F4EDE8;
--sidebar-text-primary: #575279;
--sidebar-text-secondary: #797593;
--sidebar-accent: #286983;
```

**深色主题 (Rosé Pine Moon)**：
```css
--sidebar-bg: #1B1E28;
--sidebar-bg-hover: #232633;
--sidebar-bg-active: #2A2E3D;
--sidebar-text-primary: #A6ACCD;
--sidebar-accent: #ADD7FF;
```

### 文件清单

**新增：**
- `src/layouts/AppLayout.vue` — 统一顶级布局
- `src/layouts/components/AppSidebar.vue` — 侧边栏容器
- `src/layouts/components/SidebarHeader.vue` — 组织切换
- `src/layouts/components/SidebarQuickActions.vue` — 快捷入口（Inbox/My Issues/Search）
- `src/layouts/components/SidebarAdmin.vue` — 管理菜单区
- `src/layouts/components/SidebarFooter.vue` — 底部用户 + 主题切换
- `src/composables/useSidebarTheme.ts` — 主题切换逻辑
- `src/i18n/locales/zh-CN/sidebar.ts`
- `src/i18n/locales/en-US/sidebar.ts`

**修改：**
- `src/styles/theme.css` — 新增 Rosé Pine 双主题侧边栏变量
- `src/router/routes.ts` — 统一路由结构（admin 嵌入 AppLayout）

**废弃（功能已迁移）：**
- `WorkspaceLayout.vue` → 功能迁移到 AppLayout
- `DefaultLayout.vue` → admin 路由改用 AppLayout
- `Sidebar.vue` → 功能迁移到 SidebarAdmin

---

## Phase 2: 命令面板 Cmd+K（待实施）

### 组件结构
```
CommandPalette.vue (Teleport to body, 全局遮罩)
├── 搜索输入框 (autofocus)
├── 结果列表 (分组: 最近访问 / 卡片 / 视图 / 导航 / 操作)
└── 底部快捷键提示 (↑↓ Enter Esc)
```

### 核心逻辑 — `src/composables/useCommandPalette.ts`
- `Cmd+K` / `Ctrl+K` 全局监听，toggle 面板
- `Esc` 关闭
- 搜索防抖 200ms
- 本地搜索：视图（从菜单树缓存）、导航命令、操作命令
- 远程搜索：卡片（调用后端 API）
- 键盘导航：↑↓ 选择，Enter 执行
- 最近访问：localStorage 存储最近 10 条

### 后端 API
card-service 新增：
```
POST /api/v1/cards/search
Request: { keyword: string, pageSize?: number }
Response: PageResult<{ id, title, code, cardTypeName, status }>
```

### 内置命令
```typescript
// 导航命令
'跳转到 Inbox' → router.push('/inbox')
'跳转到我的任务' → router.push('/my-issues')
'打开管理后台' → router.push('/admin')
'成员管理' → router.push('/admin/members')

// 操作命令
'切换组织' → router.push('/select-org')
'切换侧边栏主题' → toggleSidebarTheme()
```

### 文件清单

**新增：**
- `src/components/command-palette/CommandPalette.vue` — 主组件
- `src/components/command-palette/CommandResultItem.vue` — 结果项
- `src/composables/useCommandPalette.ts` — 核心逻辑
- `src/i18n/locales/zh-CN/commandPalette.ts`
- `src/i18n/locales/en-US/commandPalette.ts`
- 后端: `CardSearchController.java` + `CardSearchRequest.java` + `CardSearchResult.java`

**修改：**
- `src/layouts/AppLayout.vue` — 挂载 CommandPalette 组件
- `src/api/card.ts` — 新增 search 方法

---

## Phase 3: 多团队模式（已改为内置__PLANKA_EINST__）

### 数据模型（无独立 Team 表）

- **Team / Project / Issue**：各为 `EntityCardType`（`systemType=true`），**不**在 schema 上显式 `parentTypeIds` 继承 `{orgId}:any-trait`（由运行时/模型隐式视为任意卡）；字段直接挂在__PLANKA_EINST__上。
- **内置字段**（名称统一用**卡片标题**，不单独建 name/title 字段）：Team（identifier、color）；Project（identifier、status 枚举）；Issue（priority、status 枚举）。产品用语为 **Issue**，不使用「工单」。
- **内置关联**：`team-member`（团队 ↔ 成员特征类型）、`team-lead`（团队 → 成员，**负责人**，单选）、`project-lead`（项目 → 成员，**负责人**，单选）、`team-project`（团队 → 项目，项目侧单选可空）、`project-issue`（项目 → 工作项，工作项侧单选可空）。
- **__PLANKA_EINST__显示名（中文）**：团队、项目、工作项（`code` 仍为 `team` / `project` / `issue`）。

组织创建时由 user-service `BuiltinCardTypeService` 写入 schema；存量组织由 `BuiltinCardTypeMigrationRunner` 在启动时补建（以 `{orgId}:team` 是否存在为判据）。

### 后端

- `planka-common`：`SystemSchemaIds` 扩展 Team/Project/Issue 及 link、field ID。
- `user-service`：`BuiltinCardTypeService.java`（合并原成员特征类型/成员类型、任意卡特征类型与创建人/存档人/回收人、Team/Project/Issue）；`OrganizationService.createOrganization` 中按序调用；`BuiltinCardTypeMigrationRunner.java`。
- **不新增** `TeamController` / `sys_team` 表；列表与 CRUD 走现有 **card-service** 分页查询与卡片 API。

### 前端（Linear 式侧栏）

- **工作空间**：`/workspace/issues`、`/workspace/projects`、`/workspace/teams`（组织级聚合）；成员入口 `/admin/members`；视图入口仍为 `/workspace`。
- **你的团队**：`SidebarYourTeams` + `SidebarTeamItem`；`/team/:teamId/issues|projects`；`/project/:projectId/issues`；`+ 创建团队` 调用卡片创建并写 `team-member` 关联。
- **API**：`src/api/team.ts`（基于 `cardApi.pageQuery` + `LinkCondition`）；`src/constants/systemSchemaIds.ts`；`src/stores/teamNav.ts`。

### 文件清单（实现）

**后端新增/修改：**

- `planka-shared/planka-common/.../SystemSchemaIds.java`
- `planka-services/user-service/.../BuiltinCardTypeService.java`
- `planka-services/user-service/.../BuiltinCardTypeMigrationRunner.java`
- `OrganizationService.java`、`OrganizationRepository.java`

**前端新增/修改：**

- `planka-ui/src/api/team.ts`、`src/constants/systemSchemaIds.ts`、`src/utils/card-query.ts`
- `planka-ui/src/stores/teamNav.ts`
- `planka-ui/src/layouts/components/SidebarWorkspace.vue`、`SidebarYourTeams.vue`、`SidebarTeamItem.vue`
- `planka-ui/src/layouts/components/AppSidebar.vue`
- `planka-ui/src/views/workspace/WorkspaceIssuesView.vue`、`WorkspaceProjectsView.vue`、`WorkspaceTeamsView.vue`、`EntityCardTable.vue`
- `planka-ui/src/views/team/TeamIssuesView.vue`、`TeamProjectsView.vue`
- `planka-ui/src/views/project/ProjectIssuesView.vue`
- `planka-ui/src/router/routes.ts`、`src/types/card.ts`（查询条件使用完整 `Condition`）
- i18n：`sidebar`、`workspaceList`

---

## Phase 4: Inbox 收件箱（搁置）

原方案依赖独立通知服务与 `system_notification` 数据表。当前工程已移除通知相关后端与初始化脚本，收件箱能力需在重新设计数据与 API 后再实施。
