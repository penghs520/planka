package dev.planka.extension.history.mapper;

import dev.planka.extension.history.model.CardHistoryMetaEntity;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

/**
 * 卡片历史表元数据 Mapper
 */
@Mapper
public interface CardHistoryMetaMapper extends BaseMapper<CardHistoryMetaEntity> {

    /**
     * 根据卡片类型ID查询元数据
     */
    @Select("SELECT * FROM card_history_meta WHERE card_type_id = #{cardTypeId}")
    CardHistoryMetaEntity findByCardTypeId(String cardTypeId);
}
