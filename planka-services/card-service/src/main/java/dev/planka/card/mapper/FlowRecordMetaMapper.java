package dev.planka.card.mapper;

import dev.planka.card.service.flowrecord.FlowRecordMetaEntity;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/**
 * 流动记录元数据 Mapper
 */
@Mapper
public interface FlowRecordMetaMapper extends BaseMapper<FlowRecordMetaEntity> {

    /**
     * 根据价值流ID查询元数据
     */
    @Select("SELECT * FROM flow_record_meta WHERE stream_id = #{streamId}")
    FlowRecordMetaEntity findByStreamId(@Param("streamId") String streamId);
}
