package dev.planka.extension.comment.model;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.time.LocalDateTime;

/**
 * 评论#卡片引用实体
 */
@TableName("comment_card_ref")
public class CommentCardRefEntity {

    @TableId(type = IdType.INPUT)
    private Long id;

    private Long commentId;

    private String orgId;

    private String refCardId;

    private String refCardCode;

    private int startOffset;

    private int endOffset;

    private LocalDateTime createdAt;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getCommentId() {
        return commentId;
    }

    public void setCommentId(Long commentId) {
        this.commentId = commentId;
    }

    public String getOrgId() {
        return orgId;
    }

    public void setOrgId(String orgId) {
        this.orgId = orgId;
    }

    public String getRefCardId() {
        return refCardId;
    }

    public void setRefCardId(String refCardId) {
        this.refCardId = refCardId;
    }

    public String getRefCardCode() {
        return refCardCode;
    }

    public void setRefCardCode(String refCardCode) {
        this.refCardCode = refCardCode;
    }

    public int getStartOffset() {
        return startOffset;
    }

    public void setStartOffset(int startOffset) {
        this.startOffset = startOffset;
    }

    public int getEndOffset() {
        return endOffset;
    }

    public void setEndOffset(int endOffset) {
        this.endOffset = endOffset;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
