package dev.planka.domain.link;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

/**
 * 关联属性ID值对象
 * <p>
 * 格式: "{linkTypeId}:{SOURCE|TARGET}"
 * 例如: "263671031548350464:SOURCE"
 * <p>
 * 用于标识一个关联属性，由关联类型ID和关联位置组成。
 *
 * @param value 完整的关联属性ID字符串
 */
public record LinkFieldId(@JsonValue String value) {

    @JsonCreator
    public LinkFieldId {
        if (value == null || value.isEmpty()) {
            throw new IllegalArgumentException("LinkFieldId不能为空");
        }
        if (!LinkFieldIdUtils.isValidFormat(value)) {
            throw new IllegalArgumentException("无效的LinkFieldId格式: " + value + "，期望格式为 {linkTypeId}:{SOURCE|TARGET}");
        }
    }

    /**
     * 通过关联类型ID和位置创建 LinkFieldId
     *
     * @param linkTypeId 关联类型ID
     * @param position   关联位置
     * @return LinkFieldId 实例
     */
    public static LinkFieldId of(String linkTypeId, LinkPosition position) {
        return new LinkFieldId(LinkFieldIdUtils.build(linkTypeId, position));
    }

    /**
     * 通过关联类型ID和位置创建 LinkFieldId
     *
     * @param linkTypeId 关联类型ID
     * @param position   关联位置
     * @return LinkFieldId 实例
     */
    public static LinkFieldId of(LinkTypeId linkTypeId, LinkPosition position) {
        return new LinkFieldId(LinkFieldIdUtils.build(linkTypeId, position));
    }

    /**
     * 获取关联类型ID字符串
     *
     * @return 关联类型ID
     */
    public String getLinkTypeId() {
        return LinkFieldIdUtils.getLinkTypeId(value);
    }

    /**
     * 获取关联类型ID对象
     *
     * @return 关联类型ID对象
     */
    public LinkTypeId getLinkTypeIdObject() {
        return LinkFieldIdUtils.getLinkTypeIdObject(value);
    }

    /**
     * 获取关联位置
     *
     * @return 关联位置枚举
     */
    public LinkPosition getPosition() {
        return LinkFieldIdUtils.getPosition(value);
    }

    @Override
    public String toString() {
        return value;
    }
}
