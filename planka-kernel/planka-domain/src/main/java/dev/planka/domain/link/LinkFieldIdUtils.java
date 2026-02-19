package dev.planka.domain.link;

/**
 * 关联属性ID工具类
 * <p>
 * 关联属性ID格式: "{linkTypeId}:{SOURCE|TARGET}"
 * 例如: "263671031548350464:SOURCE"
 */
public final class LinkFieldIdUtils {

    private static final char SEPARATOR = ':';

    private LinkFieldIdUtils() {
        // 工具类，禁止实例化
    }

    /**
     * 构建关联属性ID
     *
     * @param linkTypeId 关联类型ID
     * @param position   关联位置
     * @return 格式为 "linkTypeId:POSITION" 的字符串
     */
    public static String build(String linkTypeId, LinkPosition position) {
        if (linkTypeId == null || linkTypeId.isEmpty()) {
            throw new IllegalArgumentException("linkTypeId不能为空");
        }
        if (position == null) {
            throw new IllegalArgumentException("position不能为空");
        }
        return linkTypeId + SEPARATOR + position.name();
    }

    /**
     * 构建关联属性ID
     *
     * @param linkTypeId 关联类型ID
     * @param position   关联位置
     * @return 格式为 "linkTypeId:POSITION" 的字符串
     */
    public static String build(LinkTypeId linkTypeId, LinkPosition position) {
        if (linkTypeId == null) {
            throw new IllegalArgumentException("linkTypeId不能为空");
        }
        return build(linkTypeId.value(), position);
    }

    /**
     * 从关联属性ID中提取关联类型ID
     *
     * @param linkFieldId 关联属性ID
     * @return 关联类型ID字符串
     */
    public static String getLinkTypeId(String linkFieldId) {
        if (linkFieldId == null || linkFieldId.isEmpty()) {
            throw new IllegalArgumentException("linkFieldId不能为空");
        }
        int idx = linkFieldId.lastIndexOf(SEPARATOR);
        return idx > 0 ? linkFieldId.substring(0, idx) : linkFieldId;
    }

    /**
     * 从关联属性ID中提取关联类型ID对象
     *
     * @param linkFieldId 关联属性ID
     * @return 关联类型ID对象
     */
    public static LinkTypeId getLinkTypeIdObject(String linkFieldId) {
        return LinkTypeId.of(getLinkTypeId(linkFieldId));
    }

    /**
     * 从关联属性ID中提取关联位置
     *
     * @param linkFieldId 关联属性ID
     * @return 关联位置枚举
     */
    public static LinkPosition getPosition(String linkFieldId) {
        if (linkFieldId == null || linkFieldId.isEmpty()) {
            throw new IllegalArgumentException("linkFieldId不能为空");
        }
        int idx = linkFieldId.lastIndexOf(SEPARATOR);
        if (idx > 0 && idx < linkFieldId.length() - 1) {
            String positionStr = linkFieldId.substring(idx + 1);
            try {
                return LinkPosition.valueOf(positionStr);
            } catch (IllegalArgumentException e) {
                throw new IllegalArgumentException("无效的关联位置: " + positionStr);
            }
        }
        throw new IllegalArgumentException("无效的linkFieldId格式: " + linkFieldId);
    }

    /**
     * 判断是否为有效的关联属性ID格式
     *
     * @param linkFieldId 待验证的字符串
     * @return 是否为有效格式
     */
    public static boolean isValidFormat(String linkFieldId) {
        if (linkFieldId == null || linkFieldId.isEmpty()) {
            return false;
        }
        int idx = linkFieldId.lastIndexOf(SEPARATOR);
        if (idx <= 0 || idx >= linkFieldId.length() - 1) {
            return false;
        }
        String positionStr = linkFieldId.substring(idx + 1);
        return "SOURCE".equals(positionStr) || "TARGET".equals(positionStr);
    }
}
