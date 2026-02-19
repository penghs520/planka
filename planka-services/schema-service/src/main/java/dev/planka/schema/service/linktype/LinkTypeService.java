package dev.planka.schema.service.linktype;

import dev.planka.api.schema.request.CreateSchemaRequest;
import dev.planka.api.schema.request.UpdateSchemaRequest;
import dev.planka.api.schema.request.linktype.CreateLinkTypeRequest;
import dev.planka.api.schema.request.linktype.UpdateLinkTypeRequest;
import dev.planka.api.schema.vo.cardtype.CardTypeInfo;
import dev.planka.api.schema.vo.linktype.LinkTypeOptionVO;
import dev.planka.api.schema.vo.linktype.LinkTypeVO;
import dev.planka.common.exception.CommonErrorCode;
import dev.planka.common.result.Result;
import dev.planka.domain.card.CardTypeId;
import dev.planka.domain.link.LinkPosition;
import dev.planka.domain.link.LinkTypeId;
import dev.planka.domain.schema.SchemaType;
import dev.planka.domain.schema.definition.SchemaDefinition;
import dev.planka.domain.schema.definition.cardtype.AbstractCardType;
import dev.planka.domain.schema.definition.cardtype.CardTypeDefinition;
import dev.planka.domain.schema.definition.link.LinkTypeDefinition;
import dev.planka.schema.repository.SchemaRepository;
import dev.planka.schema.service.common.SchemaCommonService;
import dev.planka.schema.service.common.SchemaQuery;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 关联类型服务
 * <p>
 * 提供关联类型的 CRUD 操作和查询功能。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class LinkTypeService {

    private final SchemaRepository schemaRepository;
    private final SchemaCommonService schemaCommonService;
    private final SchemaQuery schemaQuery;

    /**
     * 查询关联类型列表
     *
     * @param orgId 组织 ID
     * @return 关联类型列表
     */
    public Result<List<LinkTypeVO>> listLinkTypes(String orgId) {
        List<SchemaDefinition<?>> schemas = schemaQuery.query(orgId, SchemaType.LINK_TYPE);

        // 收集所有卡片类型 ID
        Set<String> allCardTypeIds = new HashSet<>();
        for (SchemaDefinition<?> schema : schemas) {
            if (schema instanceof LinkTypeDefinition linkType) {
                if (linkType.getSourceCardTypeIds() != null) {
                    linkType.getSourceCardTypeIds().forEach(id -> allCardTypeIds.add(id.value()));
                }
                if (linkType.getTargetCardTypeIds() != null) {
                    linkType.getTargetCardTypeIds().forEach(id -> allCardTypeIds.add(id.value()));
                }
            }
        }

        // 批量查询卡片类型名称
        Map<String, String> cardTypeNameMap = getCardTypeNameMap(allCardTypeIds);

        List<LinkTypeVO> voList = schemas.stream()
                .filter(s -> s instanceof LinkTypeDefinition)
                .map(s -> toVO((LinkTypeDefinition) s, cardTypeNameMap))
                .collect(Collectors.toList());

        return Result.success(voList);
    }

    /**
     * 查询关联类型选项列表（用于下拉框）
     *
     * @param orgId 组织 ID
     * @return 关联类型选项列表
     */
    public Result<List<LinkTypeOptionVO>> listLinkTypeOptions(String orgId) {
        List<SchemaDefinition<?>> schemas = schemaQuery.query(orgId, SchemaType.LINK_TYPE);

        List<LinkTypeOptionVO> options = schemas.stream()
                .filter(s -> s instanceof LinkTypeDefinition)
                .map(s -> (LinkTypeDefinition) s)
                .filter(LinkTypeDefinition::isEnabled)
                .map(this::toOptionVO)
                .collect(Collectors.toList());

        return Result.success(options);
    }

    /**
     * 根据 ID 获取关联类型详情
     *
     * @param linkTypeId 关联类型 ID
     * @return 关联类型详情
     */
    public Result<LinkTypeVO> getLinkTypeById(String linkTypeId) {
        Optional<SchemaDefinition<?>> schemaOpt = schemaRepository.findById(linkTypeId);
        if (schemaOpt.isEmpty()) {
            return Result.failure(CommonErrorCode.DATA_NOT_FOUND, "关联类型不存在: " + linkTypeId);
        }

        SchemaDefinition<?> schema = schemaOpt.get();
        if (!(schema instanceof LinkTypeDefinition linkType)) {
            return Result.failure(CommonErrorCode.BAD_REQUEST, "非关联类型: " + linkTypeId);
        }

        // 收集卡片类型 ID
        Set<String> cardTypeIds = collectCardTypeIds(linkType);
        Map<String, String> cardTypeNameMap = getCardTypeNameMap(cardTypeIds);

        return Result.success(toVO(linkType, cardTypeNameMap));
    }

    /**
     * 创建关联类型
     *
     * @param orgId      组织 ID
     * @param operatorId 操作人ID
     * @param request    创建请求
     * @return 创建后的关联类型
     */
    @Transactional
    public Result<LinkTypeVO> createLinkType(String orgId, String operatorId, CreateLinkTypeRequest request) {
        // 转换并验证卡片类型ID
        List<CardTypeId> sourceCardTypeIds = toCardTypeIds(request.getSourceCardTypeIds());
        List<CardTypeId> targetCardTypeIds = toCardTypeIds(request.getTargetCardTypeIds());

        // 验证：多个卡片类型时，必须都是属性集
        Result<Void> validationResult = validateCardTypeIds(sourceCardTypeIds, "源端");
        if (!validationResult.isSuccess()) {
            return Result.failure(validationResult.getCode(), validationResult.getMessage());
        }
        validationResult = validateCardTypeIds(targetCardTypeIds, "目标端");
        if (!validationResult.isSuccess()) {
            return Result.failure(validationResult.getCode(), validationResult.getMessage());
        }

        // 构建 LinkTypeDefinition
        LinkTypeDefinition linkType = new LinkTypeDefinition(
                LinkTypeId.generate(),
                orgId,
                request.getSourceName() + "->" + request.getTargetName()
        );

        linkType.setDescription(request.getDescription());
        linkType.setSourceName(request.getSourceName());
        linkType.setTargetName(request.getTargetName());
        linkType.setSourceVisible(request.isSourceVisible());
        linkType.setTargetVisible(request.isTargetVisible());
        linkType.setSourceCardTypeIds(sourceCardTypeIds);
        linkType.setTargetCardTypeIds(targetCardTypeIds);
        linkType.setSourceMultiSelect(request.isSourceMultiSelect());
        linkType.setTargetMultiSelect(request.isTargetMultiSelect());
        linkType.setEnabled(true);

        // 调用通用创建方法（包含元数据设置、验证、保存、引用分析、changelog记录、事件发布）
        CreateSchemaRequest schemaRequest = new CreateSchemaRequest();
        schemaRequest.setDefinition(linkType);
        Result<SchemaDefinition<?>> createResult = schemaCommonService.create(orgId, operatorId, schemaRequest);

        if (!createResult.isSuccess()) {
            return Result.failure(createResult.getCode(), createResult.getMessage());
        }

        LinkTypeDefinition savedLinkType = (LinkTypeDefinition) createResult.getData();

        // 查询卡片类型名称
        Set<String> cardTypeIds = collectCardTypeIds(savedLinkType);
        Map<String, String> cardTypeNameMap = getCardTypeNameMap(cardTypeIds);

        log.info("LinkType created: id={}, name={}", savedLinkType.getId().value(), savedLinkType.getName());
        return Result.success(toVO(savedLinkType, cardTypeNameMap));
    }

    /**
     * 更新关联类型
     *
     * @param linkTypeId 关联类型 ID
     * @param operatorId 操作人ID
     * @param request    更新请求
     * @return 更新后的关联类型
     */
    @Transactional
    public Result<LinkTypeVO> updateLinkType(String linkTypeId, String operatorId, UpdateLinkTypeRequest request) {
        Optional<SchemaDefinition<?>> schemaOpt = schemaRepository.findById(linkTypeId);
        if (schemaOpt.isEmpty()) {
            return Result.failure(CommonErrorCode.DATA_NOT_FOUND, "关联类型不存在");
        }

        if (!(schemaOpt.get() instanceof LinkTypeDefinition linkType)) {
            return Result.failure(CommonErrorCode.BAD_REQUEST, "非关联类型");
        }

        // 特殊验证：卡片类型ID
        if (request.getSourceCardTypeIds() != null) {
            List<CardTypeId> sourceCardTypeIds = toCardTypeIds(request.getSourceCardTypeIds());
            Result<Void> validationResult = validateCardTypeIds(sourceCardTypeIds, "源端");
            if (!validationResult.isSuccess()) {
                return Result.failure(validationResult.getCode(), validationResult.getMessage());
            }
            linkType.setSourceCardTypeIds(sourceCardTypeIds);
        }
        if (request.getTargetCardTypeIds() != null) {
            List<CardTypeId> targetCardTypeIds = toCardTypeIds(request.getTargetCardTypeIds());
            Result<Void> validationResult = validateCardTypeIds(targetCardTypeIds, "目标端");
            if (!validationResult.isSuccess()) {
                return Result.failure(validationResult.getCode(), validationResult.getMessage());
            }
            linkType.setTargetCardTypeIds(targetCardTypeIds);
        }

        // 更新字段
        if (request.getDescription() != null) {
            linkType.setDescription(request.getDescription());
        }

        // 更新源端和目标端名称，并重新生成关联类型名称
        boolean nameNeedsUpdate = false;
        if (request.getSourceName() != null) {
            linkType.setSourceName(request.getSourceName());
            nameNeedsUpdate = true;
        }
        if (request.getTargetName() != null) {
            linkType.setTargetName(request.getTargetName());
            nameNeedsUpdate = true;
        }
        if (nameNeedsUpdate) {
            linkType.setName(linkType.getSourceName() + "->" + linkType.getTargetName());
        }
        if (request.getSourceVisible() != null) {
            linkType.setSourceVisible(request.getSourceVisible());
        }
        if (request.getTargetVisible() != null) {
            linkType.setTargetVisible(request.getTargetVisible());
        }
        if (request.getSourceMultiSelect() != null) {
            linkType.setSourceMultiSelect(request.getSourceMultiSelect());
        }
        if (request.getTargetMultiSelect() != null) {
            linkType.setTargetMultiSelect(request.getTargetMultiSelect());
        }
        if (request.getEnabled() != null) {
            linkType.setEnabled(request.getEnabled());
        }

        // 调用通用更新方法（包含乐观锁检查、验证、保存、引用分析、changelog记录、事件发布）
        UpdateSchemaRequest schemaRequest = new UpdateSchemaRequest();
        schemaRequest.setDefinition(linkType);
        schemaRequest.setExpectedVersion(request.getExpectedVersion());
        Result<SchemaDefinition<?>> updateResult = schemaCommonService.update(linkTypeId, operatorId, schemaRequest);

        if (!updateResult.isSuccess()) {
            return Result.failure(updateResult.getCode(), updateResult.getMessage());
        }

        LinkTypeDefinition savedLinkType = (LinkTypeDefinition) updateResult.getData();

        // 查询卡片类型名称
        Set<String> cardTypeIds = collectCardTypeIds(savedLinkType);
        Map<String, String> cardTypeNameMap = getCardTypeNameMap(cardTypeIds);

        log.info("LinkType updated: id={}, version={}", savedLinkType.getId().value(), savedLinkType.getContentVersion());
        return Result.success(toVO(savedLinkType, cardTypeNameMap));
    }

    /**
     * 删除关联类型
     *
     * @param linkTypeId 关联类型 ID
     * @param operatorId 操作人ID
     * @return 操作结果
     */
    @Transactional
    public Result<Void> deleteLinkType(String linkTypeId, String operatorId) {
        return schemaCommonService.delete(linkTypeId, operatorId);
    }

    /**
     * 查询卡片类型可用的关联类型
     *
     * @param cardTypeId 卡片类型 ID
     * @param position   关联位置（可选）
     * @return 可用的关联类型列表
     */
    public Result<List<LinkTypeOptionVO>> getAvailableLinkTypes(String cardTypeId, LinkPosition position) {
        // 获取卡片类型
        Optional<SchemaDefinition<?>> cardTypeOpt = schemaRepository.findById(cardTypeId);
        if (cardTypeOpt.isEmpty() || !(cardTypeOpt.get() instanceof CardTypeDefinition)) {
            return Result.failure(CommonErrorCode.DATA_NOT_FOUND, "卡片类型不存在");
        }

        CardTypeDefinition cardType = (CardTypeDefinition) cardTypeOpt.get();
        String orgId = cardType.getOrgId();

        // 查询所有关联类型（需要全量数据来判断可用性，忽略空间过滤）
        List<SchemaDefinition<?>> allLinkTypes = schemaQuery.queryAll(orgId, SchemaType.LINK_TYPE);

        // 筛选可用的关联类型
        List<LinkTypeOptionVO> availableOptions = allLinkTypes.stream()
                .filter(s -> s instanceof LinkTypeDefinition)
                .map(s -> (LinkTypeDefinition) s)
                .filter(LinkTypeDefinition::isEnabled)
                .filter(lt -> isLinkTypeAvailableForCardType(lt, cardTypeId, position))
                .map(this::toOptionVO)
                .collect(Collectors.toList());

        return Result.success(availableOptions);
    }

    /**
     * 检查关联类型是否对卡片类型可用
     */
    private boolean isLinkTypeAvailableForCardType(LinkTypeDefinition linkType, String cardTypeId, LinkPosition position) {
        if (position == null) {
            // 如果没有指定位置，检查源端或目标端是否可用
            return isAvailableAsSource(linkType, cardTypeId) || isAvailableAsTarget(linkType, cardTypeId);
        }

        return switch (position) {
            case SOURCE -> isAvailableAsSource(linkType, cardTypeId);
            case TARGET -> isAvailableAsTarget(linkType, cardTypeId);
        };
    }

    private boolean isAvailableAsSource(LinkTypeDefinition linkType, String cardTypeId) {
        List<CardTypeId> sourceIds = linkType.getSourceCardTypeIds();
        if (sourceIds == null || sourceIds.isEmpty()) {
            return true;
        }
        return sourceIds.stream().anyMatch(id -> id.value().equals(cardTypeId));
    }

    private boolean isAvailableAsTarget(LinkTypeDefinition linkType, String cardTypeId) {
        List<CardTypeId> targetIds = linkType.getTargetCardTypeIds();
        if (targetIds == null || targetIds.isEmpty()) {
            return true;
        }
        return targetIds.stream().anyMatch(id -> id.value().equals(cardTypeId));
    }

    /**
     * 批量获取卡片类型名称映射
     */
    private Map<String, String> getCardTypeNameMap(Set<String> cardTypeIds) {
        if (cardTypeIds == null || cardTypeIds.isEmpty()) {
            return Collections.emptyMap();
        }
        List<SchemaDefinition<?>> cardTypes = schemaRepository.findByIds(cardTypeIds);
        return cardTypes.stream()
                .collect(Collectors.toMap(
                        s -> s.getId().value(),
                        SchemaDefinition::getName,
                        (a, b) -> a
                ));
    }

    /**
     * 将关联类型转换为 VO
     */
    private LinkTypeVO toVO(LinkTypeDefinition linkType, Map<String, String> cardTypeNameMap) {
        // 构建源端卡片类型信息
        List<CardTypeInfo> sourceCardTypes = null;
        if (linkType.getSourceCardTypeIds() != null && !linkType.getSourceCardTypeIds().isEmpty()) {
            sourceCardTypes = linkType.getSourceCardTypeIds().stream()
                    .map(id -> CardTypeInfo.builder()
                            .id(id.value())
                            .name(cardTypeNameMap.getOrDefault(id.value(), id.value()))
                            .build())
                    .collect(Collectors.toList());
        }

        // 构建目标端卡片类型信息
        List<CardTypeInfo> targetCardTypes = null;
        if (linkType.getTargetCardTypeIds() != null && !linkType.getTargetCardTypeIds().isEmpty()) {
            targetCardTypes = linkType.getTargetCardTypeIds().stream()
                    .map(id -> CardTypeInfo.builder()
                            .id(id.value())
                            .name(cardTypeNameMap.getOrDefault(id.value(), id.value()))
                            .build())
                    .collect(Collectors.toList());
        }

        return LinkTypeVO.builder()
                .id(linkType.getId().value())
                .orgId(linkType.getOrgId())
                .name(linkType.getName())
                .description(linkType.getDescription())
                .sourceName(linkType.getSourceName())
                .targetName(linkType.getTargetName())
                .sourceVisible(linkType.isSourceVisible())
                .targetVisible(linkType.isTargetVisible())
                .sourceCardTypes(sourceCardTypes)
                .targetCardTypes(targetCardTypes)
                .sourceMultiSelect(linkType.isSourceMultiSelect())
                .targetMultiSelect(linkType.isTargetMultiSelect())
                .systemLinkType(linkType.isSystemLinkType())
                .enabled(linkType.isEnabled())
                .contentVersion(linkType.getContentVersion())
                .createdAt(linkType.getCreatedAt())
                .updatedAt(linkType.getUpdatedAt())
                .build();
    }

    /**
     * 将关联类型转换为选项 VO
     */
    private LinkTypeOptionVO toOptionVO(LinkTypeDefinition linkType) {
        return LinkTypeOptionVO.builder()
                .id(linkType.getId().value())
                .name(linkType.getName())
                .sourceName(linkType.getSourceName())
                .targetName(linkType.getTargetName())
                .sourceVisible(linkType.isSourceVisible())
                .targetVisible(linkType.isTargetVisible())
                .sourceMultiSelect(linkType.isSourceMultiSelect())
                .targetMultiSelect(linkType.isTargetMultiSelect())
                .build();
    }

    /**
     * 将字符串列表转换为 CardTypeId 列表
     */
    private List<CardTypeId> toCardTypeIds(List<String> ids) {
        if (ids == null || ids.isEmpty()) {
            return null;
        }
        return ids.stream()
                .map(CardTypeId::of)
                .collect(Collectors.toList());
    }

    /**
     * 收集关联类型中的卡片类型ID
     */
    private Set<String> collectCardTypeIds(LinkTypeDefinition linkType) {
        Set<String> cardTypeIds = new HashSet<>();
        if (linkType.getSourceCardTypeIds() != null) {
            linkType.getSourceCardTypeIds().forEach(id -> cardTypeIds.add(id.value()));
        }
        if (linkType.getTargetCardTypeIds() != null) {
            linkType.getTargetCardTypeIds().forEach(id -> cardTypeIds.add(id.value()));
        }
        return cardTypeIds;
    }

    /**
     * 验证卡片类型ID列表
     * <p>
     * 仅当都为属性集时才支持指定多个，表示多个属性集的共同实体类型才有此关联关系
     *
     * @param cardTypeIds 卡片类型ID列表
     * @param position    位置描述（用于错误消息）
     * @return 验证结果
     */
    private Result<Void> validateCardTypeIds(List<CardTypeId> cardTypeIds, String position) {
        if (cardTypeIds == null || cardTypeIds.size() <= 1) {
            return Result.success();
        }

        // 多个卡片类型时，必须都是属性集
        Set<String> ids = cardTypeIds.stream()
                .map(CardTypeId::value)
                .collect(Collectors.toSet());
        List<SchemaDefinition<?>> schemas = schemaRepository.findByIds(ids);

        for (SchemaDefinition<?> schema : schemas) {
            if (!(schema instanceof AbstractCardType)) {
                return Result.failure(CommonErrorCode.VALIDATION_ERROR,
                        position + "指定多个卡片类型时，必须都是属性集，但 " + schema.getName() + " 不是属性集");
            }
        }

        return Result.success();
    }
}
