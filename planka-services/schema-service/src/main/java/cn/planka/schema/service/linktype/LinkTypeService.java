package cn.planka.schema.service.linktype;

import cn.planka.api.schema.request.CreateSchemaRequest;
import cn.planka.api.schema.request.UpdateSchemaRequest;
import cn.planka.api.schema.request.linktype.CreateLinkTypeRequest;
import cn.planka.api.schema.request.linktype.UpdateLinkTypeRequest;
import cn.planka.api.schema.vo.cardtype.CardTypeInfo;
import cn.planka.api.schema.vo.linktype.LinkTypeOptionVO;
import cn.planka.api.schema.vo.linktype.LinkTypeVO;
import cn.planka.common.exception.CommonErrorCode;
import cn.planka.common.result.Result;
import cn.planka.domain.card.CardTypeId;
import cn.planka.domain.link.LinkPosition;
import cn.planka.domain.link.LinkTypeId;
import cn.planka.domain.schema.SchemaType;
import cn.planka.domain.schema.definition.SchemaDefinition;
import cn.planka.domain.schema.definition.cardtype.CardTypeDefinition;
import cn.planka.domain.schema.definition.link.LinkTypeDefinition;
import cn.planka.schema.repository.SchemaRepository;
import cn.planka.schema.service.common.SchemaCommonService;
import cn.planka.schema.service.common.SchemaQuery;
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

        // 收集所有实体类型 ID
        Set<String> allCardTypeIds = new HashSet<>();
        for (SchemaDefinition<?> schema : schemas) {
            if (schema instanceof LinkTypeDefinition linkType) {
                if (linkType.getSourceCardTypeId() != null) {
                    allCardTypeIds.add(linkType.getSourceCardTypeId().value());
                }
                if (linkType.getTargetCardTypeId() != null) {
                    allCardTypeIds.add(linkType.getTargetCardTypeId().value());
                }
            }
        }

        // 批量查询实体类型名称
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

        // 收集实体类型 ID
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
        CardTypeId sourceCardTypeId = toCardTypeId(request.getSourceCardTypeId());
        CardTypeId targetCardTypeId = toCardTypeId(request.getTargetCardTypeId());

        // 构建 LinkTypeDefinition
        LinkTypeDefinition linkType = new LinkTypeDefinition(
                LinkTypeId.generate(),
                orgId,
                request.getSourceName() + "->" + request.getTargetName()
        );

        linkType.setDescription(request.getDescription());
        linkType.setSourceName(request.getSourceName());
        linkType.setSourceCode(request.getSourceCode());
        linkType.setTargetName(request.getTargetName());
        linkType.setTargetCode(request.getTargetCode());
        linkType.setSourceCardTypeId(sourceCardTypeId);
        linkType.setTargetCardTypeId(targetCardTypeId);
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

        // 查询实体类型名称
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

        if (request.getSourceCardTypeId() != null) {
            linkType.setSourceCardTypeId(toCardTypeId(request.getSourceCardTypeId()));
        }
        if (request.getTargetCardTypeId() != null) {
            linkType.setTargetCardTypeId(toCardTypeId(request.getTargetCardTypeId()));
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
        if (request.getSourceCode() != null) {
            linkType.setSourceCode(request.getSourceCode());
        }
        if (request.getTargetName() != null) {
            linkType.setTargetName(request.getTargetName());
            nameNeedsUpdate = true;
        }
        if (request.getTargetCode() != null) {
            linkType.setTargetCode(request.getTargetCode());
        }
        if (nameNeedsUpdate) {
            linkType.setName(linkType.getSourceName() + "->" + linkType.getTargetName());
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

        // 查询实体类型名称
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
     * 查询实体类型可用的关联类型
     *
     * @param cardTypeId 实体类型 ID
     * @param position   关联位置（可选）
     * @return 可用的关联类型列表
     */
    public Result<List<LinkTypeOptionVO>> getAvailableLinkTypes(String cardTypeId, LinkPosition position) {
        // 获取实体类型
        Optional<SchemaDefinition<?>> cardTypeOpt = schemaRepository.findById(cardTypeId);
        if (cardTypeOpt.isEmpty() || !(cardTypeOpt.get() instanceof CardTypeDefinition)) {
            return Result.failure(CommonErrorCode.DATA_NOT_FOUND, "实体类型不存在");
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
     * 检查关联类型是否对实体类型可用
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
        CardTypeId sourceId = linkType.getSourceCardTypeId();
        if (sourceId == null) {
            return true;
        }
        return sourceId.value().equals(cardTypeId);
    }

    private boolean isAvailableAsTarget(LinkTypeDefinition linkType, String cardTypeId) {
        CardTypeId targetId = linkType.getTargetCardTypeId();
        if (targetId == null) {
            return true;
        }
        return targetId.value().equals(cardTypeId);
    }

    /**
     * 批量获取实体类型名称映射
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
        CardTypeInfo sourceCardType = null;
        if (linkType.getSourceCardTypeId() != null) {
            CardTypeId id = linkType.getSourceCardTypeId();
            sourceCardType = CardTypeInfo.builder()
                    .id(id.value())
                    .name(cardTypeNameMap.getOrDefault(id.value(), id.value()))
                    .build();
        }

        CardTypeInfo targetCardType = null;
        if (linkType.getTargetCardTypeId() != null) {
            CardTypeId id = linkType.getTargetCardTypeId();
            targetCardType = CardTypeInfo.builder()
                    .id(id.value())
                    .name(cardTypeNameMap.getOrDefault(id.value(), id.value()))
                    .build();
        }

        return LinkTypeVO.builder()
                .id(linkType.getId().value())
                .orgId(linkType.getOrgId())
                .name(linkType.getName())
                .description(linkType.getDescription())
                .sourceName(linkType.getSourceName())
                .sourceCode(linkType.getSourceCode())
                .targetName(linkType.getTargetName())
                .targetCode(linkType.getTargetCode())
                .sourceCardType(sourceCardType)
                .targetCardType(targetCardType)
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
                .sourceMultiSelect(linkType.isSourceMultiSelect())
                .targetMultiSelect(linkType.isTargetMultiSelect())
                .build();
    }

    /**
     * 将请求中的字符串转为 CardTypeId；null 或空白视为 null（不限制）
     */
    private CardTypeId toCardTypeId(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        return CardTypeId.of(raw.trim());
    }

    /**
     * 收集关联类型中的实体类型ID
     */
    private Set<String> collectCardTypeIds(LinkTypeDefinition linkType) {
        Set<String> cardTypeIds = new HashSet<>();
        if (linkType.getSourceCardTypeId() != null) {
            cardTypeIds.add(linkType.getSourceCardTypeId().value());
        }
        if (linkType.getTargetCardTypeId() != null) {
            cardTypeIds.add(linkType.getTargetCardTypeId().value());
        }
        return cardTypeIds;
    }
}
