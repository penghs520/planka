package cn.planka.comment.repository;

import cn.planka.comment.mapper.CommentCardRefMapper;
import cn.planka.comment.model.CommentCardRefEntity;
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
