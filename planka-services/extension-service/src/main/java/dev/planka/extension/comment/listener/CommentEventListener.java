package dev.planka.extension.comment.listener;

import dev.planka.api.comment.dto.CommentDTO;
import dev.planka.api.comment.request.CreateCommentRequest;
import dev.planka.event.comment.CommentCreationRequestedEvent;
import dev.planka.event.comment.CommentEvent;
import dev.planka.extension.comment.service.CommentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

/**
 * 评论事件监听器
 *
 * <p>
 * 监听评论相关事件，处理评论创建请求。
 * 由业务规则触发的评论创建通过事件机制解耦，避免直接服务调用。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CommentEventListener {

    private final CommentService commentService;

    /**
     * 监听评论创建请求事件
     */
    @KafkaListener(topics = "kanban-comment-events", groupId = "comment-service")
    public void handleCommentEvent(CommentEvent event) {
        try {
            if (event instanceof CommentCreationRequestedEvent e) {
                handleCommentCreationRequested(e);
            } else {
                log.debug("忽略未处理的评论事件类型: {}", event.getEventType());
            }
        } catch (Exception e) {
            log.error("处理评论事件失败: {}", event, e);
        }
    }

    /**
     * 处理评论创建请求
     *
     * <p>
     * 将事件转换为创建评论请求并调用评论服务。
     * 业务规则触发的评论不包含@提及和卡片引用。
     */
    private void handleCommentCreationRequested(CommentCreationRequestedEvent event) {
        log.debug("收到评论创建请求事件: cardId={}, operatorId={}", event.getCardId(), event.getOperatorId());

        CreateCommentRequest request = new CreateCommentRequest(
            event.getCardId(),
            event.getCardTypeId(),
            null, // parentId - 业务规则触发的评论是顶级评论
            null, // replyToMemberId
            event.getContent(),
            null, // mentions - 业务规则触发的评论不包含@提及
            null, // cardRefs - 业务规则触发的评论不包含卡片引用
            event.getOperationSource() // 操作来源（业务规则信息）
        );

        CommentDTO comment = commentService.createComment(
            event.getOrgId(),
            event.getOperatorId(),
            request
        );

        log.info("业务规则触发评论创建成功: commentId={}, cardId={}", comment.id(), event.getCardId());
    }
}
