package dev.planka.extension.comment.mapper;

import dev.planka.extension.comment.model.CommentEntity;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/**
 * 评论 Mapper
 */
@Mapper
public interface CommentMapper extends BaseMapper<CommentEntity> {
}
