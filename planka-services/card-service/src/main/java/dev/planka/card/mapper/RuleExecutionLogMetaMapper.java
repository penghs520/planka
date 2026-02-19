package dev.planka.card.mapper;

import dev.planka.card.service.rule.log.RuleExecutionLogMetaEntity;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

/**
 * 规则执行日志元数据 Mapper
 */
@Mapper
public interface RuleExecutionLogMetaMapper extends BaseMapper<RuleExecutionLogMetaEntity> {

    /**
     * 根据卡片类型ID查询
     */
    @Select("SELECT * FROM biz_rule_execution_log_meta WHERE card_type_id = #{cardTypeId}")
    RuleExecutionLogMetaEntity findByCardTypeId(String cardTypeId);
}
