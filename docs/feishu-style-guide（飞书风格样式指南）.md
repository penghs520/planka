# 飞书风格样式指南

> 本项目前端采用飞书（Lark/Feishu）视觉风格，基于 **Arco Design Vue** 框架进行主题覆盖。
> 样式实现分布在以下文件中：
> - 主题变量：`planka-ui/src/styles/theme.css`
> - UnoCSS 配置：`planka-ui/uno.config.ts`
> - 全局组件覆盖：`planka-ui/src/style.css`
> - 管理表格样式：`planka-ui/src/styles/admin-table.scss`

---

## 1. 技术栈

| 层级 | 技术 | 说明 |
|------|------|------|
| UI 框架 | Arco Design Vue | 基础组件库 |
| 原子化 CSS | UnoCSS | 支持属性化模式 + 快捷方式 |
| 主题覆盖 | CSS Variables | 通过 `theme.css` 覆盖 Arco 默认变量 |

---

## 2. 色彩体系

### 2.1 品牌主色（飞书蓝）

| 用途 | 变量 | 色值 | 示例 |
|------|------|------|------|
| 主色 | `--color-primary` | `#3370FF` | 按钮、链接、选中态 |
| 悬停 | `--color-primary-hover` | `#4E83FD` | 鼠标悬停 |
| 按下 | `--color-primary-active` | `#245BDB` | 点击反馈 |
| 浅色 | `--color-primary-light` | `#E8F3FF` | 选中背景 |
| 更浅 | `--color-primary-lighter` | `#F0F5FF` | 微弱背景提示 |

主色梯度（Arco RGB 格式，`--primary-1` ~ `--primary-10`）：

| 级别 | RGB 值 | HEX 近似值 |
|------|--------|-----------|
| 1 | `240, 245, 255` | `#F0F5FF` |
| 2 | `232, 243, 255` | `#E8F3FF` |
| 3 | `194, 218, 255` | `#C2DAFF` |
| 4 | `148, 191, 255` | `#94BFFF` |
| 5 | `78, 131, 253` | `#4E83FD` |
| 6 | `51, 112, 255` | `#3370FF` |
| 7 | `36, 91, 219` | `#245BDB` |
| 8 | `26, 72, 184` | `#1A48B8` |
| 9 | `18, 55, 148` | `#123794` |
| 10 | `12, 40, 112` | `#0C2870` |

### 2.2 功能色

| 语义 | 变量 | 默认 | 悬停 | 按下 | 浅色背景 |
|------|------|------|------|------|----------|
| 成功 | `--color-success` | `#34C759` | `#4CD964` | `#2DB84D` | `#E8F8ED` |
| 警告 | `--color-warning` | `#FF9500` | `#FFAA33` | `#E68600` | `#FFF7E6` |
| 危险 | `--color-danger` | `#F54A45` | `#F76560` | `#D93F3A` | `#FEECE8` |

### 2.3 中性色 — 文本

| 用途 | 变量 | 色值 | 说明 |
|------|------|------|------|
| 主文本 | `--color-text-1` | `#1F2329` | 标题、正文 |
| 次要文本 | `--color-text-2` | `#646A73` | 描述、辅助信息 |
| 辅助文本 | `--color-text-3` | `#8F959E` | placeholder、提示 |
| 禁用文本 | `--color-text-4` | `#BBBFC4` | 不可操作文本 |

### 2.4 中性色 — 填充/背景

| 用途 | 变量 | 色值 | 说明 |
|------|------|------|------|
| 页面背景 | `--color-fill-1` / `--color-bg-2` | `#F5F6F7` | 页面底色 |
| 容器背景 | `--color-fill-2` / `--color-bg-3` | `#F0F1F2` | 卡片、面板底色 |
| 悬停背景 | `--color-fill-3` | `#DEE0E3` | 列表项 hover |
| 禁用背景 | `--color-fill-4` / `--color-bg-5` | `#C9CDD4` | 禁用控件 |
| 容器白色 | `--color-bg-1` | `#FFFFFF` | 主内容区 |
| 分隔背景 | `--color-bg-4` | `#E5E6EB` | 分割线 |

### 2.5 中性色 — 边框

| 用途 | 变量 | 色值 | 说明 |
|------|------|------|------|
| 常规边框 | `--color-border-1` | `#E5E6EB` | 卡片、输入框边框 |
| 深边框 | `--color-border-2` | `#C9CDD4` | 强调分隔 |
| 浅边框 | `--color-border-3` | `#F2F3F5` | 内部分割 |

---

## 3. 字体

### 3.1 字体族

```css
font-family: "PingFang SC", "HarmonyOS Sans SC", "Microsoft YaHei",
             -apple-system, BlinkMacSystemFont, sans-serif;
```

macOS 优先使用苹方，HarmonyOS 设备使用鸿蒙字体，Windows 回退微软雅黑。

### 3.2 字号体系

| 名称 | 字号 | 行高 | 用途 |
|------|------|------|------|
| `xs` | 12px | 18px | 辅助标签、表格紧凑字体 |
| `sm` | 13px | 20px | 次要内容、按钮小号 |
| `base` | 14px | 22px | 正文默认 |
| `lg` | 16px | 24px | 小标题、模态框标题 |
| `xl` | 18px | 28px | 区域标题 |
| `2xl` | 20px | 30px | 页面标题 |
| `3xl` | 24px | 36px | 大标题 |

### 3.3 渲染优化

```css
-webkit-font-smoothing: antialiased;
-moz-osx-font-smoothing: grayscale;
font-synthesis: none;
text-rendering: optimizeLegibility;
```

---

## 4. 圆角

| 名称 | 变量 | 值 | 适用组件 |
|------|------|----|----------|
| 小圆角 | `--radius-sm` | `4px` | 标签、内部元素 |
| 中圆角 | `--radius-md` | `6px` | 按钮、输入框、选择器 |
| 大圆角 | `--radius-lg` | `8px` | 卡片、下拉菜单、弹出层 |
| 超大圆角 | `--radius-xl` | `12px` | 模态框、抽屉 |

Arco Design 覆盖映射：

```css
--border-radius-small: 4px;   /* Arco small → 飞书 sm */
--border-radius-medium: 6px;  /* Arco medium → 飞书 md */
--border-radius-large: 8px;   /* Arco large → 飞书 lg */
```

---

## 5. 阴影

| 名称 | 变量 | 值 | 适用场景 |
|------|------|----|----------|
| 微弱 | `--shadow-sm` | `0 1px 2px rgba(31,35,41, 0.05)` | 输入框聚焦 |
| 中等 | `--shadow-md` | `0 2px 8px rgba(31,35,41, 0.08)` | 卡片 |
| 明显 | `--shadow-lg` | `0 4px 14px rgba(31,35,41, 0.1)` | 下拉菜单、弹出层 |
| 强烈 | `--shadow-xl` | `0 8px 24px rgba(31,35,41, 0.12)` | 模态框 |
| 最大 | UnoCSS `xl` | `0 16px 48px rgba(31,35,41, 0.16)` | 特殊强调场景 |

阴影色统一使用 `rgba(31, 35, 41, alpha)`（基于主文本色 `#1F2329`），避免纯黑。

---

## 6. 间距体系

基于 4px 基准网格：

| 级别 | 值 | 常用场景 |
|------|----|----------|
| 0.5 | 2px | 微间距 |
| 1 | 4px | 图标与文字间距 |
| 1.5 | 6px | 紧凑内边距 |
| 2 | 8px | 列表项间距、小按钮内边距 |
| 3 | 12px | 表格单元格内边距 |
| 4 | 16px | 卡片内边距、标准间距 |
| 5 | 20px | 区块间距 |
| 6 | 24px | 模态框内边距 |
| 8 | 32px | 大区块分隔 |

---

## 7. 组件样式规范

### 7.1 按钮

**主按钮**使用渐变背景：

```css
/* 默认 */
background: linear-gradient(135deg, #4E83FD 0%, #3370FF 100%);

/* 悬停 */
background: linear-gradient(135deg, #5C8FFE 0%, #4580FF 100%);
box-shadow: 0 4px 12px rgba(51, 112, 255, 0.3);

/* 按下 */
background: linear-gradient(135deg, #3370FF 0%, #245BDB 100%);
```

**按钮尺寸**：

| 尺寸 | 高度 | 内边距 | 字号 |
|------|------|--------|------|
| 小号 `btn-sm` | 28px (h-7) | 12px (px-3) | 12px |
| 默认 `btn` | 32px (h-8) | 16px (px-4) | 13px (sm) |
| 大号 `btn-lg` | 40px (h-10) | 24px (px-6) | 14px (base) |
| 表格小号 | 24px | 8px | 12px |
| 表格迷你 | 20px | 6px | 11px |

### 7.2 输入框 / 选择器

- 圆角：`4px`（全局覆盖）
- 边框色：`--color-border-2`（`#C9CDD4`）
- 悬停：边框变为 `--color-border-3`
- 聚焦：边框变为主色 `#3370FF`，外发光 `0 0 0 2px rgba(51, 112, 255, 0.1)`
- 禁用：背景 `--color-fill-2`，文本 `--color-text-4`
- 错误：边框变为危险色，聚焦发光使用危险色
- placeholder 色：`--color-text-3`（`#8F959E`）

### 7.3 卡片

- 圆角：`8px`（`--radius-lg`）
- 阴影：`--shadow-md`
- 白色背景

### 7.4 下拉菜单 / 弹出层

- 圆角：`8px`（`--radius-lg`）
- 阴影：`--shadow-lg`
- 选项圆角：`2px`，左右留 `4px` 边距
- 选中项背景：`rgba(51, 112, 255, 0.1)`，文字变为主色

### 7.5 模态框

- 圆角：`12px`（`--radius-xl`）
- 阴影：`--shadow-xl`
- 内边距：`24px`
- 标题：`16px`，`font-weight: 600`，左对齐
- 底部无边框线
- 确认删除弹窗：确认按钮用危险色，取消按钮用主色渐变

### 7.6 抽屉

- 左侧圆角：`12px 0 0 12px`

### 7.7 Tooltip

- 白色背景 + 边框 `--color-border-2`
- 阴影：`0 4px 12px rgba(0, 0, 0, 0.1)`
- 字号：`12px`，内边距 `8px 12px`
- 圆角：`6px`

### 7.8 Switch 开关

- 全局使用小号：`28px × 16px`
- 圆点：`12px × 12px`
- 关闭态：`--color-fill-4`
- 开启态：主色 `#3370FF`

### 7.9 管理表格

- 表头：`padding: 8px 12px`，`font-size: 12px`，不换行
- 单元格：`padding: 6px 12px`，`font-size: 13px`，长文本换行
- 排序图标：默认透明，悬停时 `opacity: 0.4`，激活时 `opacity: 1` 且变主色
- 隐藏固定列阴影
- 容器采用 flex 纵向布局，表体区域 `flex: 1` 填充剩余空间

---

## 8. UnoCSS 快捷方式

### 8.1 按钮

| 快捷方式 | 效果 |
|----------|------|
| `btn-primary` | 主色按钮（蓝底白字） |
| `btn-secondary` | 次要按钮（灰底黑字） |
| `btn-outline` | 线框按钮（白底，hover 变主色） |
| `btn-danger` | 危险按钮（红底白字） |
| `btn-text` | 文字按钮（透明底，hover 灰底） |

### 8.2 卡片

| 快捷方式 | 效果 |
|----------|------|
| `card` | 白底 + 大圆角 + 阴影 |
| `card-bordered` | 白底 + 大圆角 + 边框（无阴影） |
| `card-header` | flex 头部，底部边框分隔 |
| `card-body` | 标准内边距 `16px` |
| `card-footer` | flex 底部，顶部边框分隔，右对齐 |

### 8.3 表单

| 快捷方式 | 效果 |
|----------|------|
| `form-label` | 标签文本（`13px`、深色、粗体） |
| `form-hint` | 提示文本（`12px`、灰色） |
| `form-error` | 错误文本（`12px`、红色） |

### 8.4 布局

| 快捷方式 | 效果 |
|----------|------|
| `flex-center` | 水平垂直居中 |
| `flex-between` | 两端对齐 |
| `flex-start` | 左对齐 |
| `flex-end` | 右对齐 |
| `flex-col-center` | 纵向居中 |

### 8.5 其他

| 快捷方式 | 效果 |
|----------|------|
| `truncate-1` | 单行省略 |
| `truncate-2` | 两行省略 |
| `truncate-3` | 三行省略 |
| `divider` | 水平分隔线 |
| `divider-v` | 垂直分隔线 |

### 8.6 属性化模式

支持将 UnoCSS 类写为 HTML 属性：

```html
<div flex items-center justify-between p="4">
  <span text="sm text-2">内容</span>
</div>
```

---

## 9. 使用指南

### 9.1 优先级

1. 使用 UnoCSS 快捷方式（`btn-primary`、`card` 等）
2. 使用 UnoCSS 原子类（`flex`、`p-4`、`text-sm` 等）
3. 使用 CSS 变量（`var(--color-primary)` 等）
4. 最后考虑自定义 CSS

### 9.2 禁止事项

- 禁止硬编码色值，必须使用 CSS 变量或 UnoCSS 主题色
- 禁止硬编码中文文本，必须使用 i18n

### 9.3 示例

```html
<!-- 标准页面布局 -->
<div class="admin-table-container">
  <div class="admin-table-header">
    <a-input-search style="width: 240px" />
    <div class="admin-table-header-spacer" />
    <a-button class="create-btn" size="small">
      {{ t('common.action.create') }}
    </a-button>
  </div>
  <div class="admin-table-spin">
    <a-table :data="data" :columns="columns" />
  </div>
</div>

<!-- 卡片组件 -->
<div class="card">
  <div class="card-header">
    <span class="text-lg font-medium">{{ t('title') }}</span>
  </div>
  <div class="card-body">
    内容
  </div>
  <div class="card-footer">
    <button class="btn-secondary">{{ t('common.action.cancel') }}</button>
    <button class="btn-primary">{{ t('common.action.save') }}</button>
  </div>
</div>
```
