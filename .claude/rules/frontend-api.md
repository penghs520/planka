# 前端 API 调用规范

## 接口定义位置

所有 API 接口定义放在 `planka-ui/src/api/` 目录：

```
api/
├── user.ts          # 用户相关
├── card.ts          # 卡片相关
├── view.ts          # 视图相关
├── schema.ts        # Schema 相关
└── types.ts         # 通用类型定义
```

## 接口函数命名

- 使用驼峰命名
- 前缀表示操作类型：
  - `get` - 获取单个
  - `list` - 获取列表
  - `create` - 创建
  - `update` - 更新
  - `delete` - 删除
  - `search` - 搜索

```typescript
// 示例
export function getCardById(id: string) { ... }
export function listCardsByView(viewId: string) { ... }
export function createCard(data: CreateCardRequest) { ... }
export function updateCard(id: string, data: UpdateCardRequest) { ... }
export function deleteCard(id: string) { ... }
```

## 错误处理

统一使用拦截器处理，组件层不单独处理 HTTP 错误：

```typescript
// 在请求拦截器中统一处理
apiClient.interceptors.response.use(
  (response) => response,
  (error) => {
    // 统一处理 401、403、500 等错误
    // 显示 Arco Message 提示
    return Promise.reject(error);
  }
);
```

## 类型定义

- Request/Response 类型必须定义
- 使用 `zod` 或 TypeScript 接口做运行时校验

```typescript
export interface GetCardResponse {
  id: string;
  title: string;
  // ...
}
```
