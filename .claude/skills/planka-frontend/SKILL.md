---
name: planka-frontend
description: 为 planka 项目开发前端功能。使用 Vue 3 + TypeScript + Arco Design + UnoCSS 技术栈，遵循飞书风格设计规范。当用户要求开发前端页面、组件或功能时激活。
---

# planka 前端开发 Skill

## 技术栈
- Vue 3.5 + TypeScript 5.x
- Arco Design Vue 2.x
- UnoCSS（原子化 CSS）
- Pinia 3.x（状态管理）
- Vue Router 4
- vue-i18n 11.x

## 开发流程

### 1. 分析需求
- 理解功能目标和用户场景
- 确认是否需要新增路由
- 确认是否需要调用新 API

### 2. 检查现有资源
- 使用 Glob 查找类似组件
- 检查 `planka-ui/src/components/common/` 是否有可复用组件
- 检查 `planka-ui/src/hooks/` 是否有可用 hooks

### 3. 样式规范（必须遵守）
- 使用飞书风格配色（主色 #3370FF）
- 使用 UnoCSS 快捷方式（btn-primary, card, flex-center 等）
- 圆角：输入框 6px，卡片 8px，弹窗 12px
- 字体：PingFang SC, HarmonyOS Sans SC, Microsoft YaHei

### 4. 国际化（强制）
- **禁止硬编码中文**
- Key 命名规范：`module.feature.action`
- 在 `planka-ui/src/i18n/locales/zh-CN/` 和 `en-US/` 中添加

### 5. 组件开发
- 单文件 < 600 行
- Props 使用 TypeScript 接口定义
- 复杂逻辑抽离到 composables

### 6. 测试
- 工具函数必须写单元测试
- 通用组件必须写测试
- 使用 Vitest + Vue Test Utils

## 常用路径
- 组件：`planka-ui/src/components/`
- 页面：`planka-ui/src/views/`
- API：`planka-ui/src/api/`
- Hooks：`planka-ui/src/hooks/`
- 样式：`planka-ui/src/styles/`
- i18n：`planka-ui/src/i18n/locales/`

## 常用命令
```bash
cd planka-ui
pnpm dev          # 开发服务器
pnpm build        # 构建
pnpm test:run     # 运行测试
pnpm lint         # 自动修复 lint
pnpm lint:check   # 检查 lint
```
