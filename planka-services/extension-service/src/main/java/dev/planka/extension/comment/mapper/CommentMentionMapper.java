package dev.planka.extension.comment.mapper;

import dev.planka.extension.comment.model.CommentMentionEntity;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/**
 * 评论@提及 Mapper
 */
@Mapper
public interface CommentMentionMapper extends BaseMapper<CommentMentionEntity> {
}
