package dev.planka.card.service.flowrecord;

import dev.planka.domain.schema.definition.stream.StatusConfig;
import dev.planka.domain.schema.definition.stream.StepConfig;
import dev.planka.domain.schema.definition.stream.ValueStreamDefinition;
import dev.planka.domain.stream.StatusId;
import dev.planka.domain.stream.StatusWorkType;
import dev.planka.domain.stream.StepId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

/**
 * 价值流查询辅助服务
 * <p>
 * 提供回滚检测和 StatusWorkType 推断功能
 */
@Service
public class ValueStreamHelper {

    private static final Logger logger = LoggerFactory.getLogger(ValueStreamHelper.class);


    /**
     * 判断是否为回滚移动
     * <p>
     * 回滚定义：从后面的阶段/状态移动到前面的阶段/状态
     *
     * @param valueStream  价值流定义
     * @param fromStepId   原阶段ID
     * @param fromStatusId 原状态ID
     * @param toStepId     目标阶段ID
     * @param toStatusId   目标状态ID
     * @return true 表示回滚，false 表示正常前进
     */
    public boolean isRollback(ValueStreamDefinition valueStream,
                              StepId fromStepId, StatusId fromStatusId,
                              StepId toStepId, StatusId toStatusId) {
        if (valueStream == null || valueStream.getStepList() == null) {
            return false;
        }

        // 计算原位置和目标位置的全局排序值
        int fromOrder = calculateGlobalOrder(valueStream, fromStepId, fromStatusId);
        int toOrder = calculateGlobalOrder(valueStream, toStepId, toStatusId);

        // 如果目标位置在原位置之前，则为回滚
        return toOrder < fromOrder;
    }

    /**
     * 获取状态的工作类型
     *
     * @param valueStream 价值流定义
     * @param stepId      阶段ID
     * @param statusId    状态ID
     * @return 状态工作类型
     */
    public StatusWorkType getStatusWorkType(ValueStreamDefinition valueStream, StepId stepId, StatusId statusId) {
        if (valueStream == null || valueStream.getStepList() == null) {
            return StatusWorkType.WORKING; // 默认返回 WORKING
        }

        // 查找状态配置
        for (StepConfig step : valueStream.getStepList()) {
            if (step.getId().equals(stepId) && step.getStatusList() != null) {
                for (StatusConfig status : step.getStatusList()) {
                    if (status.getId().equals(statusId)) {
                        return status.getWorkType() != null ? status.getWorkType() : StatusWorkType.WORKING;
                    }
                }
            }
        }

        // 如果找不到，尝试只通过 statusId 查找（兼容处理）
        for (StepConfig step : valueStream.getStepList()) {
            if (step.getStatusList() != null) {
                for (StatusConfig status : step.getStatusList()) {
                    if (status.getId().equals(statusId)) {
                        return status.getWorkType() != null ? status.getWorkType() : StatusWorkType.WORKING;
                    }
                }
            }
        }

        return StatusWorkType.WORKING; // 默认返回 WORKING
    }

    /**
     * 根据状态ID查找所属的阶段ID
     *
     * @param valueStream 价值流定义
     * @param statusId    状态ID
     * @return 阶段ID
     */
    public static Optional<StepId> findStepIdByStatusId(ValueStreamDefinition valueStream, StatusId statusId) {
        if (valueStream == null || valueStream.getStepList() == null) {
            return Optional.empty();
        }

        for (StepConfig step : valueStream.getStepList()) {
            if (step.getStatusList() != null) {
                for (StatusConfig status : step.getStatusList()) {
                    if (status.getId().equals(statusId)) {
                        return Optional.of(step.getId());
                    }
                }
            }
        }

        return Optional.empty();
    }


    public static StatusId getFirstStatusId(ValueStreamDefinition valueStream) {
        for (StepConfig step : valueStream.getStepList()) {
            if (step.getStatusList() != null) {
                for (StatusConfig status : step.getStatusList()) {
                    return status.getId();
                }
            }
        }
        throw new IllegalStateException("valueStream : " + valueStream.getId().value() + " has no any status");
    }

    /**
     * 获取从起始状态到目标状态之间的状态路径
     * <p>
     * 返回的路径包含起始状态和目标状态，按价值流顺序排列。
     * 如果是回滚移动，路径会按照回滚方向排列（从后向前）。
     *
     * @param valueStream  价值流定义
     * @param fromStatusId 起始状态ID（可为null，表示新建卡片）
     * @param toStatusId   目标状态ID
     * @return 状态路径列表
     */
    public List<StatusNode> getStatusPath(ValueStreamDefinition valueStream,
                                          StatusId fromStatusId,
                                          StatusId toStatusId) {
        if (valueStream == null || valueStream.getStepList() == null) {
            return Collections.emptyList();
        }

        // 构建所有状态的有序列表
        List<StatusNode> allStatuses = buildOrderedStatusList(valueStream);

        if (allStatuses.isEmpty()) {
            return Collections.emptyList();
        }

        // 新建卡片场景：只返回目标状态
        if (fromStatusId == null) {
            return allStatuses.stream()
                    .filter(node -> node.statusId().equals(toStatusId))
                    .toList();
        }

        // 找到起始和目标状态的位置
        int fromIndex = -1;
        int toIndex = -1;
        for (int i = 0; i < allStatuses.size(); i++) {
            if (allStatuses.get(i).statusId().equals(fromStatusId)) {
                fromIndex = i;
            }
            if (allStatuses.get(i).statusId().equals(toStatusId)) {
                toIndex = i;
            }
        }

        if (fromIndex == -1 || toIndex == -1) {
            logger.warn("状态路径查找失败: fromStatusId={}, toStatusId={}", fromStatusId, toStatusId);
            return Collections.emptyList();
        }

        // 提取路径（包含起始和目标状态）
        List<StatusNode> path;
        if (fromIndex <= toIndex) {
            // 正向移动
            path = new ArrayList<>(allStatuses.subList(fromIndex, toIndex + 1));
        } else {
            // 回滚移动：路径反向
            path = new ArrayList<>(allStatuses.subList(toIndex, fromIndex + 1));
            Collections.reverse(path);
        }

        return path;
    }

    /**
     * 构建价值流中所有状态的有序列表
     */
    private List<StatusNode> buildOrderedStatusList(ValueStreamDefinition valueStream) {
        List<StatusNode> result = new ArrayList<>();

        for (StepConfig step : valueStream.getStepList()) {
            if (step.getStatusList() != null) {
                for (StatusConfig status : step.getStatusList()) {
                    result.add(new StatusNode(
                            step.getId(),
                            status.getId(),
                            status.getWorkType() != null ? status.getWorkType() : StatusWorkType.WORKING
                    ));
                }
            }
        }

        return result;
    }


    /**
     * 状态节点，包含状态的完整信息
     */
    public record StatusNode(StepId stepId, StatusId statusId, StatusWorkType workType) {
    }

    /**
     * 获取状态名称
     *
     * @param valueStream 价值流定义
     * @param statusId    状态ID
     * @return 状态名称，未找到则返回状态ID
     */
    public String getStatusName(ValueStreamDefinition valueStream, StatusId statusId) {
        if (valueStream == null || valueStream.getStepList() == null || statusId == null) {
            return statusId != null ? statusId.value() : "";
        }

        for (StepConfig step : valueStream.getStepList()) {
            if (step.getStatusList() != null) {
                for (StatusConfig status : step.getStatusList()) {
                    if (status.getId().equals(statusId.value())) {
                        return status.getName() != null ? status.getName() : statusId.value();
                    }
                }
            }
        }

        return statusId.value();
    }

    // ==================== 私有方法 ====================

    /**
     * 计算状态的全局排序值
     * <p>
     * 全局排序值 = 阶段排序值 * 10000 + 状态排序值
     * 这样可以确保不同阶段的状态有明确的先后顺序
     */
    private int calculateGlobalOrder(ValueStreamDefinition valueStream, StepId stepId, StatusId statusId) {
        int stepOrder = 0;
        int statusOrder = 0;

        for (StepConfig step : valueStream.getStepList()) {
            if (step.getId().equals(stepId)) {
                stepOrder = step.getSortOrder();
                if (step.getStatusList() != null) {
                    for (StatusConfig status : step.getStatusList()) {
                        if (status.getId().equals(statusId)) {
                            statusOrder = status.getSortOrder();
                            break;
                        }
                    }
                }
                break;
            }
        }

        return stepOrder * 10000 + statusOrder;
    }
}
