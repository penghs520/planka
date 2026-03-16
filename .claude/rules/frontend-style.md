# 前端样式规范（飞书风格）

## 技术栈
- **UI 框架**: Arco Design Vue
- **原子化 CSS**: UnoCSS（配置见 `planka-ui/uno.config.ts`）
- **主题变量**: `planka-ui/src/styles/theme.css`

## 配色

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

## 字体

```css
font-family: "PingFang SC", "HarmonyOS Sans SC", "Microsoft YaHei", -apple-system, sans-serif;
```

## 圆角

- 输入框/按钮: `6px` (`--radius-md`)
- 卡片/下拉: `8px` (`--radius-lg`)
- 弹窗: `12px` (`--radius-xl`)

## UnoCSS 快捷方式

```html
<!-- 布局 -->
<div class="flex items-center gap-4 p-4">

<!-- 快捷方式 -->
<button class="btn-primary">保存</button>
<button class="btn-secondary">取消</button>
<button class="btn-danger">删除</button>
<div class="card">
  <div class="card-header">标题</div>
  <div class="card-body">内容</div>
</div>

<!-- 属性化模式 -->
<div flex items-center justify-between p="4">
```

常用快捷方式：`btn-primary`, `btn-secondary`, `btn-danger`, `card`, `card-header`, `card-body`, `flex-center`, `flex-between`

## 国际化（强制）

- **禁止硬编码中文**
- 语言文件：`src/i18n/locales/{zh-CN,en-US}/`
- Key 命名：`module.feature.action`，如 `common.action.save`
- 使用：
  - Vue: `{{ t('key') }}`
  - TS: `i18n.global.t('key')`
  - Arco Rule: `computed(() => [{ message: t('key') }])`
