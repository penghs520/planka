package dev.planka.extension.comment.repository;

import dev.planka.extension.comment.mapper.CommentCardRefMapper;
import dev.planka.extension.comment.model.CommentCardRefEntity;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 评论#卡片引用 Repository
 */
@Repository
public class CommentCardRefRepository {

    private final CommentCardRefMapper cardRefMapper;

    public CommentCardRefRepository(CommentCardRefMapper cardRefMapper) {
        this.cardRefMapper = cardRefMapper;
    }

    public int insert(CommentCardRefEntity entity) {
        return cardRefMapper.insert(entity);
    }

    public int delete(LambdaQueryWrapper<CommentCardRefEntity> query) {
        return cardRefMapper.delete(query);
    }

    public List<CommentCardRefEntity> selectList(LambdaQueryWrapper<CommentCardRefEntity> query) {
        return cardRefMapper.selectList(query);
    }
}
