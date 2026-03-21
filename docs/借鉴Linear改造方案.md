# 借鉴 Linear 改造方案

## 概述

在保持 planka 零代码核心优势的基础上，借鉴 Linear 的交互设计，从四个维度提升操作效率和视觉体验：界面风格、命令面板、多团队模式、Inbox 收件箱。

## 关键决策

1. **统一为一套布局** — 管理后台嵌入主布局侧边栏，不再需要独立的 DefaultLayout
2. **侧边栏支持深色/浅色切换** — Rosé Pine 配色方案，默认深色
3. **新建独立 Team 表** — `sys_team` + `sys_team_member`，不依赖 Schema 系统

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

## Phase 3: 多团队模式（待实施）

### 数据模型
```sql
CREATE TABLE sys_team (
  id BIGINT NOT NULL AUTO_INCREMENT,
  org_id BIGINT NOT NULL COMMENT '所属组织',
  name VARCHAR(100) NOT NULL COMMENT '团队名称',
  identifier VARCHAR(20) COMMENT '团队标识(如 ENG, DESIGN)',
  icon VARCHAR(50) COMMENT '图标',
  color VARCHAR(20) DEFAULT '#5E6AD2' COMMENT '主题色',
  sort_order INT DEFAULT 0 COMMENT '排序',
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  KEY idx_org_id (org_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='团队';

CREATE TABLE sys_team_member (
  id BIGINT NOT NULL AUTO_INCREMENT,
  team_id BIGINT NOT NULL,
  user_id BIGINT NOT NULL,
  role VARCHAR(20) DEFAULT 'MEMBER' COMMENT 'LEADER/MEMBER',
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  UNIQUE KEY uk_team_user (team_id, user_id),
  KEY idx_user_id (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='团队成员';
```

### 后端 API
user-service 新增 TeamController：
```
GET    /api/v1/teams              — 获取当前组织的团队列表
POST   /api/v1/teams              — 创建团队
PUT    /api/v1/teams/{id}         — 更新团队
DELETE /api/v1/teams/{id}         — 删除团队
GET    /api/v1/teams/{id}/members — 获取团队成员
POST   /api/v1/teams/{id}/members — 添加成员
DELETE /api/v1/teams/{id}/members/{userId} — 移除成员
GET    /api/v1/teams/my           — 获取我所在的团队
```

### 侧边栏展示
```
SidebarTeams.vue
└── SidebarTeamSection.vue (v-for team in myTeams)
    ├── 团队标识色块 + 团队名（可折叠）
    └── 团队成员数 + 简要信息
```

### 文件清单

**新增：**
- `docker/mysql/init/09_team_tables.sql` — 建表
- 后端: `TeamController.java`, `TeamService.java`, `TeamRepository.java`, `TeamEntity.java`, `TeamMemberEntity.java`, `TeamDTO.java`
- `src/api/team.ts` — Team API
- `src/stores/team.ts` — Team Store
- `src/layouts/components/SidebarTeams.vue` — 团队列表
- `src/layouts/components/SidebarTeamSection.vue` — 团队区块
- `src/views/admin/team/TeamListView.vue` — 团队管理页
- `src/views/admin/team/components/TeamFormModal.vue` — 编辑弹窗
- `src/i18n/locales/zh-CN/team.ts`
- `src/i18n/locales/en-US/team.ts`

**修改：**
- `src/router/routes.ts` — 新增 /admin/team 路由
- `src/layouts/components/SidebarAdmin.vue` — 新增团队管理菜单项
- `gateway-service application.yml` — 新增 team 路由

---

## Phase 4: Inbox 收件箱（待实施）

### 现状
后端已有 `system_notification` 表 + `SystemNotificationRepository`（支持分页查询、标记已读、统计未读数）。缺少 REST API 和前端页面。

### 后端 API
notification-service 新增 InboxController：
```
GET    /api/v1/inbox              — 分页查询通知（支持 isRead/category 筛选）
GET    /api/v1/inbox/unread-count — 获取未读数
PUT    /api/v1/inbox/{id}/read    — 标记单条已读
PUT    /api/v1/inbox/read-all     — 全部标记已读
DELETE /api/v1/inbox/{id}         — 删除通知
```

### 数据库增强
```sql
ALTER TABLE system_notification
  ADD COLUMN category VARCHAR(50) DEFAULT 'DEFAULT'
    COMMENT '类别: ASSIGN/MENTION/COMMENT/STATUS_CHANGE/DEFAULT',
  ADD COLUMN actor_user_id VARCHAR(32) COMMENT '触发者ID',
  ADD COLUMN actor_name VARCHAR(100) COMMENT '触发者名称';
```

### 实时通知方案
- Phase 4.1（首版）：轮询，30 秒间隔查询未读数
- Phase 4.2（增强）：SSE 推送

### 前端页面
```
InboxPage.vue
├── 顶部: 标题 + "全部标记已读"按钮
├── 筛选栏: 全部 / 未读 / 已读（Tab 切换）
├── 通知列表: InboxItem.vue (头像 + 标题 + 时间 + 已读状态)
└── 空状态: InboxEmpty.vue
```

### 文件清单

**新增：**
- 后端: `InboxController.java`, `InboxNotificationDTO.java`
- `src/api/inbox.ts` — Inbox API
- `src/stores/inbox.ts` — Inbox Store（未读数 + 轮询）
- `src/views/inbox/InboxPage.vue` — 主页面
- `src/views/inbox/components/InboxItem.vue` — 通知项
- `src/views/inbox/components/InboxFilter.vue` — 筛选器
- `src/views/inbox/components/InboxEmpty.vue` — 空状态
- `src/i18n/locales/zh-CN/inbox.ts`
- `src/i18n/locales/en-US/inbox.ts`

**修改：**
- `docker/mysql/init/08_notification_tables.sql` — 新增字段
- `SystemNotificationEntity.java` — 新增 category, actorUserId 字段
- `gateway-service application.yml` — 新增 inbox 路由
- `src/router/routes.ts` — 新增 /inbox 路由
- `src/layouts/components/SidebarQuickActions.vue` — Inbox 未读徽标
