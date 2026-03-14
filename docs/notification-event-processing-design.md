# 通知服务事件处理流程设计

## 1. 接收 Kafka 事件入口

### 1.1 Kafka 消费者配置
- **Topic**: `kanban-notification-events`
- **Consumer Group**: `notification-service-group`
- **并发消费**: 支持多分区并发处理

### 1.2 事件监听器
```java
@Component
public class NotificationEventListener {
    @KafkaListener(topics = "kanban-notification-events")
    public void handleEvent(NotificationEvent event) {
        // 委托给事件分发器
        eventDispatcher.dispatch(event);
    }
}
```

## 2. 事件分发

### 2.1 事件分发器
```java
@Component
public class NotificationEventDispatcher {
    private final Map<Class<? extends NotificationEvent>, NotificationEventHandler> handlers;

    public void dispatch(NotificationEvent event) {
        NotificationEventHandler handler = handlers.get(event.getClass());
        if (handler != null) {
            handler.handle(event);
        }
    }
}
```

### 2.2 事件处理器接口
```java
public interface NotificationEventHandler<T extends NotificationEvent> {
    void handle(T event);
    Class<T> getSupportedEventType();
}
```

## 3. 事件工厂

### 3.1 通知上下文工厂
```java
@Component
public class NotificationContextFactory {
    /**
     * 从事件创建通知上下文
     */
    public NotificationContext createContext(NotificationEvent event) {
        return NotificationContext.builder()
            .orgId(event.getOrgId())
            .triggerEvent(event.getTriggerEvent())
            .operatorId(event.getOperatorId())
            .sourceCardId(event.getCardId())
            .eventData(extractEventData(event))
            .occurredAt(event.getOccurredAt())
            .build();
    }

    private Map<String, Object> extractEventData(NotificationEvent event) {
        // 根据事件类型提取数据
        if (event instanceof CardChangedNotificationEvent) {
            return extractCardData((CardChangedNotificationEvent) event);
        }
        return Collections.emptyMap();
    }
}
```

## 4. 事件处理 - 转换为通知上下文

### 4.1 通知上下文对象
```java
@Data
@Builder
public class NotificationContext {
    private String orgId;
    private TriggerEvent triggerEvent;
    private String operatorId;
    private String sourceCardId;
    private Map<String, Object> eventData;
    private Instant occurredAt;

    // 卡片数据（用于模板解析）
    private CardSnapshot cardSnapshot;
    private CardSnapshot previousCardSnapshot;

    // 操作人数据
    private UserInfo operator;
}
```

### 4.2 卡片快照
```java
@Data
public class CardSnapshot {
    private String cardId;
    private String cardTypeId;
    private String cardTypeName;
    private Map<String, Object> fieldValues;
    private Instant snapshotAt;
}
```

## 5. 渠道分发

### 5.1 渠道分发器
```java
@Component
public class ChannelDispatcher {
    private final NotificationChannelRegistry channelRegistry;

    /**
     * 根据模板配置的渠道列表分发通知
     */
    public List<NotificationResult> dispatch(
        NotificationTemplateDefinition template,
        NotificationSendContext sendContext
    ) {
        List<NotificationResult> results = new ArrayList<>();

        for (String channelId : template.getChannels()) {
            NotificationChannel channel = channelRegistry.getChannel(channelId);
            if (channel != null) {
                NotificationRequest request = buildRequest(sendContext, channelId);
                NotificationResult result = channel.send(request);
                results.add(result);
            }
        }

        return results;
    }
}
```

## 6. 通知分发（核心转换逻辑）

### 6.1 选择通知模板

```java
@Component
public class NotificationTemplateSelector {
    private final NotificationTemplateRepository templateRepository;

    /**
     * 根据触发事件和参数类型选择匹配的模板
     */
    public List<NotificationTemplateDefinition> selectTemplates(
        NotificationContext context
    ) {
        return templateRepository.findByOrgIdAndTriggerEventAndEnabled(
            context.getOrgId(),
            context.getTriggerEvent(),
            true
        ).stream()
            .filter(template -> matchesParameter(template, context))
            .collect(Collectors.toList());
    }

    private boolean matchesParameter(
        NotificationTemplateDefinition template,
        NotificationContext context
    ) {
        DefinitionParameter param = template.getDefinitionParameter();

        if (param instanceof CardTypeDefinitionParameter) {
            CardTypeDefinitionParameter cardParam = (CardTypeDefinitionParameter) param;
            return cardParam.getCardTypeId().equals(
                context.getCardSnapshot().getCardTypeId()
            );
        }

        return true;
    }
}
```

### 6.2 应用模板

```java
@Component
public class NotificationTemplateApplier {
    private final ExpressionEvaluator expressionEvaluator;

    /**
     * 应用模板，解析表达式
     */
    public AppliedTemplate apply(
        NotificationTemplateDefinition template,
        NotificationContext context
    ) {
        // 构建表达式上下文
        Map<String, Object> expressionContext = buildExpressionContext(context);

        // 解析标题模板
        String title = expressionEvaluator.evaluate(
            template.getTitleTemplate(),
            expressionContext
        );

        // 解析内容模板
        String content = null;
        String richContent = null;

        if (template.getContent() instanceof ShortNotificationContent) {
            ShortNotificationContent shortContent =
                (ShortNotificationContent) template.getContent();
            content = expressionEvaluator.evaluate(
                shortContent.getTextTemplate(),
                expressionContext
            );
        } else if (template.getContent() instanceof LongNotificationContent) {
            LongNotificationContent longContent =
                (LongNotificationContent) template.getContent();
            richContent = expressionEvaluator.evaluate(
                longContent.getRichTextTemplate(),
                expressionContext
            );
        }

        return AppliedTemplate.builder()
            .template(template)
            .title(title)
            .content(content)
            .richContent(richContent)
            .build();
    }

    private Map<String, Object> buildExpressionContext(NotificationContext context) {
        Map<String, Object> expressionContext = new HashMap<>();

        // 添加操作人
        expressionContext.put("操作人", context.getOperator());
        expressionContext.put("operator", context.getOperator());

        // 添加卡片数据
        if (context.getCardSnapshot() != null) {
            CardSnapshot card = context.getCardSnapshot();
            expressionContext.put(card.getCardTypeName(), card.getFieldValues());

            // 添加字段值（支持 ${字段名} 格式）
            expressionContext.putAll(card.getFieldValues());
        }

        return expressionContext;
    }
}
```

### 6.3 解析目标用户

```java
@Component
public class RecipientResolver {
    private final CardRepository cardRepository;
    private final UserRepository userRepository;

    /**
     * 根据接收者选择器解析目标用户
     */
    public List<RecipientInfo> resolve(
        RecipientSelector selector,
        NotificationContext contex{
        List<RecipientInfo> recipients = new ArrayList<>();

        // 处理多选择器模式
        if (selector.getSelectors() != null && !selector.getSelectors().isEmpty()) {
            for (SelectorItem item : selector.getSelectors()) {
                recipients.addAll(resolveSelector(item, context));
            }
        } else {
            // 向后兼容：单选择器模式
            recipients.addAll(resolveLegacySelector(selector, context));
        }

        return recipients.stream()
            .distinct()
            .collect(Collectors.toList());
    }

    private List<RecipientInfo> resolveSelector(
        SelectorItem item,
        NotificationContext context
    ) {
        if (item.getSelectorType() == SelectorType.CURRENT_OPERATOR) {
            return List.of(toRecipientInfo(context.getOperator()));
        }

        if (item.getSelectorType() == SelectorType.FROM_FIELD) {
            String fieldId = item.getFieldId();
            String source = item.getSource();

            if ("OPERATOR".equals(source)) {
                // 从操作人的字段获取
                return resolveFromOperatorField(fieldId, context);
            } else {
                // 从卡片字段获取
                return resolveFromCardField(fieldId, context);
            }
        }

        return Collections.emptyList();
    }

    private List<RecipientInfo> resolveFromCardField(
        String fieldId,
        NotificationContext context
    ) {
        Object fieldValue = context.getCardSnapshot()
            .getFieldValues()
            .get(fieldId);

        if (fieldValue instanceof List) {
            // LINK 类型字段，可能是多个用户
            List<String> userIds = (List<String>) fieldValue;
            return userIds.stream()
                .map(userRepository::findById)
                .filter(Optional::isPresent)
                .map(Optional::get)
                .map(this::toRecipientInfo)
                .collect(Collectors.toList());
        }

        return Collections.emptyList();
    }
}
```

### 6.4 构建发送上下文

```java
@Data
@Builder
public class NotificationSendContext {
    private NotificationTemplateDefinition template;
    private AppliedTemplate appliedTemplate;
    private List<RecipientInfo> recipients;
    private NotificationContext originalContext;

    // 用于追踪
    private String ruleId;
    private String cardId;
    private String operatorId;
}

@Component
public class NotificationSendContextBuilder {
    /**
     * 构建发送上下文
     */
    public NotificationSendContext build(
        NotificationTemplateDefinition template,
        AppliedTemplate appliedTemplate,
        List<RecipientInfo> recipients,
        NotificationContext context
    ) {
        return NotificationSendContext.builder()
            .template(template)
            .appliedTemplate(appliedTemplate)
            .recipients(recipients)
            .originalContext(context)
            .ruleId(template.getId())
            .cardId(context.getSourceCardId())
            .operatorId(context.getOperatorId())
            .build();
    }
}
```

## 7. 渠道发送

### 7.1 通知请求构建器
```java
@Component
public class NotificationRequestBuilder {
    /**
     * 为特定渠道构建通知请求
     */
    public NotificationRequest buildRequest(
        NotificationSendContext sendContext,
        String channelId
    ) {
        AppliedTemplate applied = sendContext.getAppliedTemplate();

        NotificationRequest.Builder builder = NotificationRequest.builder()
            .orgId(sendContext.getOriginalContext().getOrgId())
            .title(applied.getTitle())
            .recipientUserIds(extractUserIds(sendContext.getRecipients()))
            .recipients(sendContext.getRecipients())
            .source(NotificationSource.builder()
                .ruleId(sendContext.getRuleId())
                .cardId(sendContext.getCardId())
                .operatorId(sendContext.getOperatorId())
                .build());

        // 根据渠道类型设置内容
        if ("email".equals(channelId)) {
            builder.richContent(applied.getRichContent());

            // 邮件渠道支持抄送
            if (applied.getTemplate().getContent() instanceof LongNotificationContent) {
                LongNotificationContent longContent =
                    (LongNotificationContent) applied.getTemplate().getContent();
                if (longContent.getCcRecipients() != null) {
                    builder.extras(Map.of("cc", longContent.getCcRecipients()));
                }
            }
        } else {
            builder.content(applied.getContent())        }

        return builder.build();
    }

    private List<String> extractUserIds(List<RecipientInfo> recipients) {
        return recipients.stream()
            .map(RecipientInfo::getUserId)
            .collect(Collectors.toList());
    }
}
```

### 7.2 通知记录保存
```java
@Component
public class NotificationRecordSaver {
    private final NotificationRecordRepository recordRepository;

    /**
     * 保存通知发送记录
     */
    public void saveRecord(
        NotificationSendContext sendContext,
        List<NotificationResult> results
    ) {
        for (NotificationResult result : res) {
            NotificationRecordEntity record = new NotificationRecordEntity();
            record.setOrgId(sendContext.getOriginalContext().getOrgId());
            record.setChannelId(result.getChannelId());
            record.setTemplateId(sendContext.getTemplate().getId());
            record.setRuleId(sendContext.getRuleId());
            record.setCardId(sendContext.getCardId());
            record.setTitle(sendContext.getAppliedTemplate().getTitle());
            record.setRecipientCount(sendContext.getRecipients().size());
            record.setSuccessCount(countSuccessful(result));
            record.setStatus(determineStatus(result));
            record.setErrorMessage(result.getErrorMessage());
            record.setCreatedAt(Instant.now());

            recordRepository.save(record);
        }
    }

    private int countSuccessful(NotificationResult result) {
        if (result.getRecipientResults() == null) {
            return result.isSuccess() ? 1 : 0;
        }
        return (int) result.getRecipientResults().stream()
            .filter(RecipientResult::isSuccess)
            .count();
    }

    private NotificationStatus determineStatus(NotificationResult result) {if (result.isSuccess()) return NotificationStatus.SUCCESS;
        if (result.getRecipientResults() != null &&
            result.getRecipientResults().stream().anyMatch(RecipientResult::isSuccess)) {
            return NotificationStatus.PARTIAL;
        }
        return NotificationStatus.FAILED;
    }
}
```

## 8. 完整流程编排

### 8.1 通知处理编排器
```java
@Component
public class NotificationProcessor {
    private final NotificationContextFactory contextFactory;
    private final NotificationTemplateSelector templateSelector;
    private final NotificationTemplateApplier templateApplier;
    private final RentResolver recipientResolver;
    private final NotificationSendContextBuilder sendContextBuilder;
    private final ChannelDispatcher channelDispatcher;
    private final NotificationRecordSaver recordSaver;

    /**
     * 处理通知事件的完整流程
     */
    @Transactional
    public void process(NotificationEvent event) {
        // 1. 创建通知上下文
        NotificationContext context = contextFactory.createContext(event);

        // 2. 选择匹配的模板
        List<NotificationTemplateDefinition> templates =
            templateSelector.selectTemplates(context);

        // 3. 对每个模板执行通知流程
        for (NotificationTemplateDefinition template : templates) {
            processTemplate(template, context);
        }
    }

    private void processTemplate(
        NotificationTemplateDefinition template,
        NotificationContext context
    ) {
        try {
            // 4. 应用模板
            AppliedTemplate appliedTemplate = templateApplier.apply(template, context);

            // 5. 解析目标用户
            List<RecipientInfo> recipients = recipientResolver.resolve(
                template.getRecipientSelector(),
                context
            );

            if (recipients.isEmpty()) {
                log.warn("No recipients found for template: mplate.getId());
                return;
            }

            // 6. 构建发送上下文
            NotificationSendContext sendContext = sendContextBuilder.build(
                template,
                appliedTemplate,
                recipients,
                context
            );

            // 7. 渠道分发
            List<NotificationResult> results = channelDispatcher.dispatch(
                template,
                sendContext
            );

            // 8. 保存发送记录
            recordSaver.saveRecord(sendContext, results);

        } catch (Exception e) {
      log.error("Failed to process template: {}", template.getId(), e);
        }
    }
}
```

## 9. 表达式解析器

### 9.1 表达式解析接口
```java
public interface ExpressionEvaluator {
    /**
     * 解析表达式模板
     * 支持格式：${操作人}、${需求.标题}、${归档人}
     */
    String evaluate(String template, Map<String, Object> context);
}
```

### 9.2 简单实现（基于正则替换）
```java
@Component
public class SimpleExpressionEvaluator implements ExpressionEvaluator {
    private static final Pattern EXPRESSION_PATTERN =
        Pattern.compile("\\$\\{([^}]+)\\}");

    @Over  public String evaluate(String template, Map<String, Object> context) {
        if (template == null) return null;

        Matcher matcher = EXPRESSION_PATTERN.matcher(template);
        StringBuffer result = new StringBuffer();

        while (matcher.find()) {
            String expression = matcher.group(1);
            Object value = resolveExpression(expression, context);
            matcher.appendReplacement(result,
                value != null ? Matcher.quoteReplacement(value.toString()) : "");
        }

        matcher.appendTail(result);
        return result.toString();
    }

    private Object resolveExpressioxpression, Map<String, Object> context) {
        // 支持点号分隔的路径：需求.标题
        String[] parts = expression.split("\\.");

        Object current = context.get(parts[0]);
        for (int i = 1; i < parts.length && current != null; i++) {
            if (current instanceof Map) {
                current = ((Map<?, ?>) current).get(parts[i]);
            } else {
                // 使用反射获取属性
                current = getProperty(current, parts[i]);
            }
        }

        return current;
    }

    private Object getProperty(Object obj, String propertyName) {
        try {
            String getterName = "get" +
                propertyName.substring(0, 1).toUpperCase() +
                propertyName.substring(1);
            Method getter = obj.getClass().getMethod(getterName);
            return getter.invoke(obj);
        } catch (Exception e) {
            return null;
        }
    }
}
```

## 10. 关键设计点

### 10.1 异步处理
- Kafka 消费者异步接收事件
- 支持批量处理和重试机制
- 失败事件记录到死信队列

### 10.2 性能优化
- 模板缓存：避免重复查询数据库
- 批量发送：支持渠道批量发送接口
- 并发处理：多个模板并行处理

### 10.3 可扩展性
- 插件化渠道：通过 SPI 机制扩展新渠道
- 表达式扩展：支持更复杂的表达式语法
- 事件类型扩展：新增事件类型只需实现 NotnEvent

### 10.4 可观测性
- 记录每次通知发送的详细日志
- 统计各渠道的成功率
- 监控通知延迟和失败率

## 11. 数据流图

```
NotificationEvent (Kafka)
    ↓
NotificationEventListener
    ↓
NotificationEventDispatcher
    ↓
NotificationEventHandler
    ↓
NotificationProcessor
    ├─→ NotificationContextFactory → NotificationContext
    ├─→ NotificationTemplateSelector → List<Template>
    └─→ For each template:
        ├─→ NotificationTemplateApplier → AppliedTemplate
        ├─→ RecipientResolver → List<RecipientInfo>
        ├─→ NotificationSendContextBuilder → NotificationSendContext
        ├─→ ChannelDispatcher
        │   └─→ For each channel:
        │       ├─→ NotificationRequestBuilder → NotificationRequest
        │       └─→ NotificationChannel.send() → NotificationResult
        └─→ NotificationRecordSaver → DB
```

## 12. 实现优先级

1. **P0 - 核心流程**
   - NotificationEventListener
   - NotificationProcessor
   - SimpleExpressionEvaluator
   - RecipientResolver

2. **P1 - 模板支持**
   - NotificationTemplateSelector
   - NotificationTemplateApplier
   - NotificationSendContextBuilder

3. **P2 - 渠道集成**
   - ChannelDispatcher
   - NotificationRequestBuilder
   - NotificationRecordSaver

4. **P3 - 优化增强**
   - 模板缓存
   - 批量发送
   - 监控告警
