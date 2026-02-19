package dev.planka.extension.comment.repository;

import dev.planka.extension.comment.mapper.CommentMapper;
import dev.planka.extension.comment.model.CommentEntity;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 评论 Repository
 */
@Repository
public class CommentRepository {

    private final CommentMapper commentMapper;

    public CommentRepository(CommentMapper commentMapper) {
        this.commentMapper = commentMapper;
    }

    public CommentEntity selectById(Long id) {
        return commentMapper.selectById(id);
    }

    public int insert(CommentEntity entity) {
        return commentMapper.insert(entity);
    }

    public int updateById(CommentEntity entity) {
        return commentMapper.updateById(entity);
    }

    public Page<CommentEntity> selectPage(Page<CommentEntity> page, LambdaQueryWrapper<CommentEntity> query) {
        return commentMapper.selectPage(page, query);
    }

    public List<CommentEntity> selectList(LambdaQueryWrapper<CommentEntity> query) {
        return commentMapper.selectList(query);
    }
}
