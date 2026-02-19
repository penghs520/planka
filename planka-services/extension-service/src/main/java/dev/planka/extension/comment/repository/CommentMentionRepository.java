package dev.planka.extension.comment.repository;

import dev.planka.extension.comment.mapper.CommentMentionMapper;
import dev.planka.extension.comment.model.CommentMentionEntity;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 评论@提及 Repository
 */
@Repository
public class CommentMentionRepository {

    private final CommentMentionMapper mentionMapper;

    public CommentMentionRepository(CommentMentionMapper mentionMapper) {
        this.mentionMapper = mentionMapper;
    }

    public int insert(CommentMentionEntity entity) {
        return mentionMapper.insert(entity);
    }

    public int delete(LambdaQueryWrapper<CommentMentionEntity> query) {
        return mentionMapper.delete(query);
    }

    public List<CommentMentionEntity> selectList(LambdaQueryWrapper<CommentMentionEntity> query) {
        return mentionMapper.selectList(query);
    }
}
