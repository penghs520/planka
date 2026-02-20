# 端到端测试 (E2E)

基于 Playwright 的端到端测试，覆盖认证、管理后台、工作区及视觉回归。

## 目录结构

```
planka-ui/e2e/
├── playwright.config.ts        # Playwright 配置
├── global-setup.ts             # 全局前置：登录 + 创建测试组织
├── global-teardown.ts          # 全局后置：删除测试组织
├── .auth/                      # 认证状态缓存（gitignore）
├── fixtures/
│   └── base.ts                 # 扩展 test 对象，注入 apiClient 等 fixture
├── helpers/
│   ├── api-client.ts           # 后端 API 直调，用于数据 setup/teardown
│   ├── selectors.ts            # Arco Design 组件选择器封装
│   └── test-data.ts            # 测试数据常量与工具函数
├── page-objects/               # Page Object Model
│   ├── login.page.ts
│   ├── select-org.page.ts
│   ├── workspace.page.ts
│   ├── card-detail.page.ts
│   └── admin/
│       ├── card-type.page.ts
│       ├── link-type.page.ts
│       └── view-config.page.ts
└── tests/                      # 测试用例
    ├── auth/                   # 登录、登出、选择组织
    ├── admin/                  # 卡片类型、关联类型、视图配置
    ├── workspace/              # 创建卡片、查看卡片、卡片详情
    ├── navigation/             # 路由跳转、组织切换
    └── visual/                 # 视觉回归测试（截图对比）
```

## 运行测试

```bash
cd planka-ui

# 运行全部 e2e 测试
pnpm e2e

# 带浏览器界面运行（调试用）
pnpm e2e:headed

# Playwright UI 模式（交互式调试）
pnpm e2e:ui

# Debug 模式（断点调试）
pnpm e2e:debug

# 只运行视觉回归测试
pnpm e2e:visual

# 更新视觉回归基准截图
pnpm e2e:visual:update

# 查看测试报告
pnpm e2e:report

# 只运行某个测试文件
pnpm e2e e2e/tests/auth/login.spec.ts

# 只运行某个 project
pnpm e2e --project=chromium
```

### 前置条件

- 前端 `pnpm dev` 运行在 `localhost:3000`（配置了 `webServer` 会自动启动，也可手动启动后复用）
- 后端网关运行在 `localhost:9000`

## 测试 Project

| Project    | 说明                         | 运行条件       |
| ---------- | ---------------------------- | -------------- |
| `chromium` | 功能测试，Chrome 内核        | 始终运行       |
| `firefox`  | 功能测试，Firefox 内核       | 仅 CI 环境     |
| `visual`   | 视觉回归测试，固定 1440×900  | 始终运行       |

## 全局 Setup / Teardown

### global-setup

1. 使用 `E2E_EMAIL` / `E2E_PASSWORD` 登录（默认 `super@planka.dev` / `changeme`）
2. 创建唯一测试组织 `E2E_{timestamp}`
3. 切换到该组织，写入 localStorage（token、orgId 等）
4. 保存浏览器 storageState 到 `.auth/user.json`，后续测试复用登录态
5. 保存 orgId + token 到 `.auth/e2e-context.json`，供 teardown 使用

### global-teardown

- 读取 `e2e-context.json`，调用 API 删除测试组织（级联清理所有数据）
- 设置 `E2E_KEEP_DATA=true` 可跳过清理，方便调试

## 分层约定

### Page Object (`page-objects/`)

封装页面交互，测试用例不直接操作选择器：

```ts
export class LoginPage {
  constructor(private page: Page) {
    this.emailInput = page.locator('.login-form-wrapper input').first()
    // ...
  }
  async login(email: string, password: string) { /* ... */ }
}
```

### Fixture (`fixtures/base.ts`)

扩展 Playwright 的 `test`，注入 `apiClient` 等公共依赖：

```ts
import { test as base } from '@playwright/test'
import { ApiClient } from '../helpers/api-client'

export const test = base.extend<E2EFixtures>({
  apiClient: async ({ request }, use) => {
    await use(new ApiClient(request))
  },
})
```

需要 API 操作的测试从 `fixtures/base` 导入 `test`，不需要的直接用 `@playwright/test`。

### Selectors (`helpers/selectors.ts`)

统一封装 Arco Design 组件选择器，避免在测试中硬编码 CSS 类名：

```ts
arco.input('名称')       // 按 label 定位输入框
arco.tableRow('需求')    // 按文本定位表格行
arco.modal               // 弹窗容器
```

## 视觉回归测试

视觉测试位于 `tests/visual/`，使用 `toHaveScreenshot()` 进行截图对比。

基准截图存放在测试文件旁的 `-snapshots/` 目录中（如 `admin.visual.spec.ts-snapshots/`）。

```bash
# 首次运行或 UI 变更后，更新基准截图
pnpm e2e:visual:update
```

截图路径由 `snapshotPathTemplate` 控制，格式为：
```
tests/visual/{testFileName}-snapshots/{arg}-{projectName}{ext}
```

## 环境变量

| 变量             | 默认值              | 说明                       |
| ---------------- | ------------------- | -------------------------- |
| `E2E_EMAIL`      | `super@planka.dev`  | 登录邮箱                   |
| `E2E_PASSWORD`   | `changeme`          | 登录密码                   |
| `E2E_KEEP_DATA`  | -                   | 设为 `true` 跳过 teardown  |
| `CI`             | -                   | CI 环境标识，启用 Firefox   |

## 编写新测试

1. 如果涉及新页面，先在 `page-objects/` 创建 Page Object
2. 在 `tests/` 对应目录下创建 `.spec.ts` 文件
3. 需要 API 操作时从 `fixtures/base` 导入 `test`
4. 选择器优先使用 `helpers/selectors.ts` 中的封装
5. 视觉测试文件命名为 `*.visual.spec.ts`，放在 `tests/visual/` 下
