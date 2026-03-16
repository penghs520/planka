package cn.planka.domain.schema.definition.condition;

/**
 * 非正向条件操作符标识接口
 * <p>
 * 实现此接口的操作符表示"非正向条件"，例如：不等于、不包含、不在范围内等。
 * 当通过 path 获取到多个目标卡片时，必须所有卡片都满足条件才返回 true。
 * <p>
 * 未实现此接口的操作符默认为"正向条件"，任一卡片满足条件即返回 true。
 */
public interface NegativeOperator {
}
