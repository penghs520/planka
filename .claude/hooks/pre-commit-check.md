# Pre-commit 检查提醒

## 自动执行（Git Hooks）

以下检查由 `.githooks/pre-commit` 自动执行：

### 前端变更时
- [ ] TypeScript 类型检查 (`vue-tsc --noEmit`)
- [ ] ESLint 检查 (`pnpm lint:check`)
- [ ] 前端单元测试 (`pnpm test:run`)
- [ ] 前端构建 (`pnpm build`)

### 后端变更时
- [ ] Maven 测试 (自动检测受影响模块并运行)

## 手动确认清单

提交前请确认：

- [ ] 未包含敏感信息（密码、密钥、token）
- [ ] 若修改了数据库初始化脚本，已考虑现有数据兼容性
- [ ] 若新增/修改了功能，同步更新了 `docs/` 文档
- [ ] 提交信息符合规范 `<type>: <description>`

## 失败排查

如果 pre-commit 失败：

1. **查看具体错误输出**

2. **前端问题**：
   - 运行 `pnpm lint` 自动修复部分问题
   - 运行 `pnpm type-check` 查看类型错误

3. **后端问题**：
   - 运行 `mvn test -pl <模块> -am` 定位具体失败测试
   - 检查测试日志中的错误堆栈

4. **严禁使用 `--no-verify` 跳过检查**

## 常见错误

| 错误 | 解决方案 |
|------|----------|
| TypeScript 类型错误 | 检查接口定义和类型推断 |
| ESLint 错误 | 运行 `pnpm lint` 自动修复 |
| 测试失败 | 检查 mock 数据和断言条件 |
| 构建失败 | 检查依赖导入和语法错误 |
