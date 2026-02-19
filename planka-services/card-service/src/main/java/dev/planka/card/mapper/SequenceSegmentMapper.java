package dev.planka.card.mapper;

import dev.planka.card.service.sequence.SequenceSegmentEntity;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.*;

/**
 * 号段分配表 Mapper
 */
@Mapper
public interface SequenceSegmentMapper extends BaseMapper<SequenceSegmentEntity> {

    /**
     * 根据组织ID和号段类型查询号段记录
     */
    @Select("SELECT * FROM sequence_segment WHERE org_id = #{orgId} AND segment_key = #{segmentKey}")
    SequenceSegmentEntity findByOrgIdAndKey(@Param("orgId") String orgId, @Param("segmentKey") String segmentKey);

    /**
     * 使用乐观锁更新号段，获取下一个号段范围
     * 
     * @return 更新的行数（1=成功，0=版本冲突需重试）
     */
    @Update("UPDATE sequence_segment SET current_max = current_max + step, version = version + 1 " +
            "WHERE org_id = #{orgId} AND segment_key = #{segmentKey} AND version = #{version}")
    int updateAndIncrementMax(@Param("orgId") String orgId,
            @Param("segmentKey") String segmentKey,
            @Param("version") int version);

    /**
     * 插入新的号段记录
     */
    @Insert("INSERT INTO sequence_segment (org_id, segment_key, current_max, step, version) " +
            "VALUES (#{orgId}, #{segmentKey}, #{step}, #{step}, 0)")
    int insertNewSegment(@Param("orgId") String orgId,
            @Param("segmentKey") String segmentKey,
            @Param("step") int step);
}
