---
name: planka-test
description: 为 planka 项目编写测试。支持前端 Vitest 测试和后端 JUnit 5 测试。当用户要求编写测试、修复测试失败或优化测试覆盖时激活。
---

# planka 测试 Skill

## 前端测试

### 框架
- Vitest
- Vue Test Utils

### 测试文件位置
- 与源文件同目录或 `__tests__` 目录
- 命名：`{name}.spec.ts`

### 工具函数测试
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

### 组件测试
```typescript
import { describe, it, expect } from 'vitest';
import { mount } from '@vue/test-utils';
import MyComponent from './MyComponent.vue';

describe('MyComponent', () => {
  it('should render correctly', () => {
    const wrapper = mount(MyComponent, {
      props: { title: 'Test' }
    });
    expect(wrapper.text()).toContain('Test');
  });
});
```

### 运行测试
```bash
cd planka-ui
pnpm test:run
```

## 后端测试

### 框架
- JUnit 5
- AssertJ

### 测试文件位置
- 与被测试类同包名
- 命名：`{ClassName}Test.java`

### Service 层测试
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

### 运行测试
```bash
# 全量测试
./mvnw test

# 特定模块
./mvnw test -pl planka-services/card-service -am
```

## 强制检查

Git Hooks 会自动运行测试，失败禁止提交。**严禁使用 `--no-verify`**。
