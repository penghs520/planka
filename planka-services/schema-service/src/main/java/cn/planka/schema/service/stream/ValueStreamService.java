package cn.planka.schema.service.stream;

import cn.planka.common.result.Result;
import cn.planka.domain.card.CardTypeId;
import cn.planka.domain.schema.SchemaType;
import cn.planka.domain.schema.definition.SchemaDefinition;
import cn.planka.domain.schema.definition.stream.ValueStreamDefinition;
import cn.planka.schema.repository.SchemaRepository;
import cn.planka.schema.service.common.SchemaQuery;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

/**
 * 价值流服务
 * <p>
 * 提供价值流定义的查询功能
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ValueStreamService {

    private final SchemaQuery schemaQuery;
    private final SchemaRepository schemaRepository;

    /**
     * 根据卡片类型ID获取价值流定义
     * <p>
     * 一个卡片类型仅允许创建一个价值流，因此返回单个结果或null
     *
     * @param cardTypeId 卡片类型ID
     * @return 价值流定义，不存在则返回null
     */
    public Result<ValueStreamDefinition> getByCardTypeId(String cardTypeId) {
        log.debug("Getting value stream by cardTypeId: {}", cardTypeId);

        List<SchemaDefinition<?>> definitions = schemaQuery.queryBySecondKey(
                CardTypeId.of(cardTypeId),
                SchemaType.VALUE_STREAM
        );

        Optional<ValueStreamDefinition> valueStream = definitions.stream()
                .filter(d -> d instanceof ValueStreamDefinition)
                .map(d -> (ValueStreamDefinition) d)
                .findFirst();

        return Result.success(valueStream.orElse(null));
    }

    /**
     * 根据ID获取价值流定义
     *
     * @param valueStreamId 价值流ID
     * @return 价值流定义
     */
    public Result<ValueStreamDefinition> getById(String valueStreamId) {
        log.debug("Getting value stream by id: {}", valueStreamId);

        Optional<SchemaDefinition<?>> definitionOpt = schemaRepository.findById(valueStreamId);
        if (definitionOpt.isPresent() && definitionOpt.get() instanceof ValueStreamDefinition valueStream) {
            return Result.success(valueStream);
        }

        return Result.success(null);
    }
}
