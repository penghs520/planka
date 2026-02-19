package dev.planka.card.service.flowrecord;

import dev.planka.domain.stream.FlowRecord;
import dev.planka.domain.stream.FlowRecordId;
import dev.planka.domain.stream.FlowRecordType;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * 流动记录生成器
 * <p>
 * 根据状态变更上下文和状态路径生成流动记录。
 * 跨多个状态移动时，会生成中间状态的流动记录。
 * <p>
 * 示例（正常移动 A → D，经过 B、C）：
 * <pre>
 * LEAVE(A), ENTER(B)
 * LEAVE(B), ENTER(C)
 * LEAVE(C), ENTER(D)
 * </pre>
 * <p>
 * 示例（回滚移动 D → A，经过 C、B）：
 * <pre>
 * ROLLBACK_LEAVE(D), ROLLBACK_ENTER(C)
 * ROLLBACK_LEAVE(C), ROLLBACK_ENTER(B)
 * ROLLBACK_LEAVE(B), ROLLBACK_ENTER(A)
 * </pre>
 */
@Component
public class FlowRecordGenerator {

    /**
     * 根据状态路径生成流动记录
     *
     * @param context    状态变更上下文
     * @param statusPath 状态路径（包含起始状态和目标状态，按移动方向排序）
     * @return 流动记录列表
     */
    public List<FlowRecord> generate(CardStatusChangeContext context,
                                     List<ValueStreamHelper.StatusNode> statusPath) {
        if (statusPath == null || statusPath.isEmpty()) {
            return List.of();
        }

        // 新建卡片场景：只有目标状态，生成一条进入记录
        if (statusPath.size() == 1) {
            ValueStreamHelper.StatusNode target = statusPath.get(0);
            FlowRecordType enterType = context.isRollback() ? FlowRecordType.ROLLBACK_ENTER : FlowRecordType.ENTER;
            return List.of(createRecord(context, target, enterType));
        }

        List<FlowRecord> records = new ArrayList<>();
        FlowRecordType leaveType = context.isRollback() ? FlowRecordType.ROLLBACK_LEAVE : FlowRecordType.LEAVE;
        FlowRecordType enterType = context.isRollback() ? FlowRecordType.ROLLBACK_ENTER : FlowRecordType.ENTER;

        // 遍历路径，生成每一对离开/进入记录
        for (int i = 0; i < statusPath.size() - 1; i++) {
            ValueStreamHelper.StatusNode from = statusPath.get(i);
            ValueStreamHelper.StatusNode to = statusPath.get(i + 1);

            records.add(createRecord(context, from, leaveType));
            records.add(createRecord(context, to, enterType));
        }

        return records;
    }

    /**
     * 创建流动记录
     */
    private FlowRecord createRecord(CardStatusChangeContext context,
                                    ValueStreamHelper.StatusNode statusNode,
                                    FlowRecordType recordType) {
        return FlowRecord.builder()
                .id(FlowRecordId.generate())
                .cardId(context.getCardId())
                .cardTypeId(context.getCardTypeId())
                .streamId(context.getStreamId())
                .stepId(statusNode.stepId())
                .statusId(statusNode.statusId())
                .statusWorkType(statusNode.workType())
                .recordType(recordType)
                .eventTime(context.getEventTime())
                .operatorId(context.getOperatorId())
                .build();
    }
}
