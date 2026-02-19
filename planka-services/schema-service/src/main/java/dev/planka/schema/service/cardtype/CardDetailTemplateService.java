package dev.planka.schema.service.cardtype;

import dev.planka.common.result.Result;
import dev.planka.domain.card.CardTypeId;
import dev.planka.domain.schema.CardDetailTemplateId;
import dev.planka.domain.schema.EntityState;
import dev.planka.domain.schema.SchemaType;
import dev.planka.domain.schema.definition.SchemaDefinition;
import dev.planka.domain.schema.definition.template.CardDetailTemplateDefinition;
import dev.planka.schema.dto.TemplateListItemVO;
import dev.planka.schema.repository.SchemaRepository;
import dev.planka.schema.service.common.SchemaQuery;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 卡片详情页模板服务
 */
@Service
@RequiredArgsConstructor
public class CardDetailTemplateService {

    private final SchemaRepository schemaRepository;
    private final SchemaQuery schemaQuery;

    /**
     * 查询模板列表
     *
     * @param orgId      组织ID
     * @param cardTypeId 卡片类型ID（可选）
     * @return 模板列表
     */
    public Result<List<TemplateListItemVO>> list(String orgId, String cardTypeId) {
        List<SchemaDefinition<?>> definitions;

        if (cardTypeId != null && !cardTypeId.isEmpty()) {
            // 按卡片类型筛选（查询全部，不应用空间过滤）
            definitions = schemaQuery.queryBySecondKey(CardTypeId.of(cardTypeId), SchemaType.CARD_DETAIL_TEMPLATE);
        } else {
            // 查询组织下所有模板（查询全部，不应用空间过滤）
            definitions = schemaQuery.queryAll(orgId, SchemaType.CARD_DETAIL_TEMPLATE);
        }

        List<TemplateListItemVO> voList = definitions.stream()
                .filter(d -> d instanceof CardDetailTemplateDefinition)
                .map(d -> toListItemVO((CardDetailTemplateDefinition) d))
                .collect(Collectors.toList());

        return Result.success(voList);
    }

    /**
     * 根据卡片类型ID获取模板列表
     *
     * @param cardTypeId 卡片类型ID
     * @return 模板定义列表
     */
    public Result<List<CardDetailTemplateDefinition>> getByCardType(String cardTypeId) {
        List<SchemaDefinition<?>> definitions = schemaQuery.queryBySecondKey(
                CardTypeId.of(cardTypeId), SchemaType.CARD_DETAIL_TEMPLATE);

        List<CardDetailTemplateDefinition> templates = definitions.stream()
                .filter(d -> d instanceof CardDetailTemplateDefinition)
                .map(d -> (CardDetailTemplateDefinition) d)
                .collect(Collectors.toList());

        return Result.success(templates);
    }

    /**
     * 复制模板
     *
     * @param templateId 源模板ID
     * @param operatorId 操作人ID
     * @param newName    新模板名称
     * @return 新模板定义
     */
    @Transactional
    public Result<CardDetailTemplateDefinition> copy(String templateId, String operatorId, String newName) {
        // 获取源模板
        SchemaDefinition<?> source = schemaRepository.findById(templateId).orElse(null);
        if (source == null) {
            return Result.failure("TEMPLATE_NOT_FOUND", "模板不存在");
        }
        if (!(source instanceof CardDetailTemplateDefinition sourceTemplate)) {
            return Result.failure("INVALID_TEMPLATE_TYPE", "无效的模板类型");
        }

        // 创建副本
        CardDetailTemplateDefinition copy = new CardDetailTemplateDefinition(
                CardDetailTemplateId.generate(),
                sourceTemplate.getOrgId(),
                newName
        );

        // 复制业务属性
        copy.setDescription(sourceTemplate.getDescription());
        copy.setCardTypeId(sourceTemplate.getCardTypeId());
        copy.setSystemTemplate(false); // 复制的模板不是系统模板
        copy.setEffectiveCondition(sourceTemplate.getEffectiveCondition());
        copy.setPriority(sourceTemplate.getPriority());
        copy.setHeader(sourceTemplate.getHeader());
        copy.setTabs(sourceTemplate.getTabs());
        copy.setEnabled(true);

        // 设置元数据
        copy.setState(EntityState.ACTIVE);
        copy.setContentVersion(1);
        copy.setStructureVersion("1.0.0");
        LocalDateTime now = LocalDateTime.now();
        copy.setCreatedAt(now);
        copy.setCreatedBy(operatorId);
        copy.setUpdatedAt(now);
        copy.setUpdatedBy(operatorId);

        // 保存
        schemaRepository.save(copy);

        return Result.success(copy);
    }

    /**
     * 转换为列表项 VO
     */
    private TemplateListItemVO toListItemVO(CardDetailTemplateDefinition definition) {
        TemplateListItemVO vo = new TemplateListItemVO();
        vo.setId(definition.getId().value());
        vo.setOrgId(definition.getOrgId());
        vo.setName(definition.getName());
        vo.setDescription(definition.getDescription());
        vo.setCardTypeId(definition.getCardTypeId() != null ? definition.getCardTypeId().value() : null);
        vo.setSystemTemplate(definition.isSystemTemplate());
        vo.setPriority(definition.getPriority());
        vo.setTabCount(definition.getTabs() != null ? definition.getTabs().size() : 0);
        vo.setEnabled(definition.isEnabled());
        vo.setDefault(definition.isDefault());
        vo.setContentVersion(definition.getContentVersion());
        vo.setCreatedAt(definition.getCreatedAt());
        vo.setUpdatedAt(definition.getUpdatedAt());
        return vo;
    }

    /**
     * 设置为默认模板
     * <p>
     * 将指定模板设置为默认模板，同时取消该卡片类型下其他模板的默认状态
     *
     * @param templateId 模板ID
     * @param operatorId 操作人ID
     * @return 操作结果
     */
    @Transactional
    public Result<Void> setDefault(String templateId, String operatorId) {
        // 获取目标模板
        SchemaDefinition<?> target = schemaRepository.findById(templateId).orElse(null);
        if (target == null) {
            return Result.failure("TEMPLATE_NOT_FOUND", "模板不存在");
        }
        if (!(target instanceof CardDetailTemplateDefinition targetTemplate)) {
            return Result.failure("INVALID_TEMPLATE_TYPE", "无效的模板类型");
        }

        // 如果已经是默认模板，直接返回成功
        if (targetTemplate.isDefault()) {
            return Result.success();
        }

        // 取消该卡片类型下其他模板的默认状态
        List<SchemaDefinition<?>> sameTypeTemplates = schemaQuery.queryBySecondKey(
                targetTemplate.getCardTypeId(),
                SchemaType.CARD_DETAIL_TEMPLATE
        );

        LocalDateTime now = LocalDateTime.now();
        for (SchemaDefinition<?> def : sameTypeTemplates) {
            if (def instanceof CardDetailTemplateDefinition template && template.isDefault()) {
                template.setDefault(false);
                template.setUpdatedBy(operatorId);
                template.setUpdatedAt(now);
                schemaRepository.save(template);
            }
        }

        // 设置目标模板为默认
        targetTemplate.setDefault(true);
        targetTemplate.setUpdatedBy(operatorId);
        targetTemplate.setUpdatedAt(now);
        schemaRepository.save(targetTemplate);

        return Result.success();
    }
}
