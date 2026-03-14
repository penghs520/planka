package cn.agilean.kanban.extension.comment.mapper;

import cn.agilean.kanban.extension.comment.model.CommentMentionEntity;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/**
 * 评论@提及 Mapper
 */
@Mapper
public interface CommentMentionMapper extends BaseMapper<CommentMentionEntity> {
}
