package dev.planka.extension.history.vo;

import lombok.Builder;
import lombok.Data;

import java.util.List;

/**
 * 历史消息 VO - 包含填充后的参数
 */
@Data
@Builder
public class HistoryMessageVO {

    /**
     * 消息码（对应国际化资源文件中的key）
     */
    private String messageKey;

    /**
     * 消息参数列表（已填充显示名称）
     */
    private List<HistoryArgumentVO> args;
}
