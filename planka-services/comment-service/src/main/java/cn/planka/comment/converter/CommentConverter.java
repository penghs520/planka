package cn.planka.comment.converter;

import cn.planka.api.comment.dto.CardRefDTO;
import cn.planka.api.comment.dto.CommentDTO;
import cn.planka.api.comment.dto.MentionDTO;
import cn.planka.event.comment.OperationSource;
import cn.planka.comment.model.CommentCardRefEntity;
import cn.planka.comment.model.CommentEntity;
import cn.planka.comment.model.CommentMentionEntity;

import java.util.List;

/**
 * 评论转换器
 */
public class CommentConverter {

    private CommentConverter() {
    }

    public static CommentDTO toDTO(CommentEntity entity,
                                   String authorName,
                                   String authorAvatar,
                                   String replyToMemberName,
                                   List<MentionDTO> mentions,
                                   List<CardRefDTO> cardRefs,
                                   List<CommentDTO> replies,
                                   OperationSource operationSource) {
        return new CommentDTO(
                entity.getId(),
                entity.getOrgId(),
                entity.getCardId(),
                entity.getCardTypeId(),
                entity.getParentId(),
                entity.getRootId(),
                entity.getReplyToMemberId(),
                replyToMemberName,
                entity.getContent(),
                entity.getStatus(),
                entity.getEditCount(),
                entity.getLastEditedAt(),
                entity.getAuthorId(),
                authorName,
                authorAvatar,
                entity.getCreatedAt(),
                entity.getUpdatedAt(),
                mentions,
                cardRefs,
                replies,
                operationSource
        );
    }

    public static MentionDTO toMentionDTO(CommentMentionEntity entity, String memberName) {
        return new MentionDTO(
                entity.getId(),
                entity.getCommentId(),
                entity.getMentionedMemberId(),
                memberName,
                entity.getStartOffset(),
                entity.getEndOffset()
        );
    }

    public static CardRefDTO toCardRefDTO(CommentCardRefEntity entity, String cardTitle) {
        return new CardRefDTO(
                entity.getId(),
                entity.getCommentId(),
                entity.getRefCardId(),
                entity.getRefCardCode(),
                cardTitle,
                entity.getStartOffset(),
                entity.getEndOffset()
        );
    }
}
