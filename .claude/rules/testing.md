# 测试规范

**强制执行**：Git Hooks 会自动运行测试，失败禁止提交。**严禁使用 `--no-verify`**。

## 后端测试

### 框架
- JUnit 5
- AssertJ

### 规范
- Service 层 Mock Repository
- 测试类与被测试类同包名
- 测试方法命名：`should{ExpectedBehavior}When{Condition}`

```java
@ExtendWith(MockitoExtension.class)
class CardServiceTest {

    @Mock
    private CardRepository cardRepository;

    @InjectMocks
    private CardService cardService;

    @Test
    void shouldReturnCardWhenIdExists() {
        // given
        String cardId = "card-123";
        CardEntity entity = new CardEntity();
        entity.setId(cardId);
        when(cardRepository.findById(cardId)).thenReturn(Optional.of(entity));

        // when
        CardDTO result = cardService.getCard(cardId);

        // then
        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(cardId);
    }
}
```

## 前端测试

### 框架
- Vitest
- Vue Test Utils

### 必须测试的内容
- 工具函数 (utils)
- 通用组件 (components/common)
- 关键业务逻辑

### 命名规范
- 测试文件：`{name}.spec.ts`
- 测试方法：`should {expected behavior} when {condition}`

```typescript
import { describe, it, expect } from 'vitest';
import { formatDate } from '@/utils/date';

describe('formatDate', () => {
  it('should format date to YYYY-MM-DD', () => {
    const date = new Date('2024-03-16');
    expect(formatDate(date)).toBe('2024-03-16');
  });
});
```

## 运行测试

```bash
# 后端
cd planka-services/card-service
mvn test

# 前端
cd planka-ui
pnpm test:run
```
