package dev.planka.extension.comment.service;

import dev.planka.api.comment.dto.CommentDTO;
import dev.planka.api.comment.request.CreateCommentRequest;
import dev.planka.api.comment.request.UpdateCommentRequest;
import dev.planka.event.comment.UserOperationSource;
import dev.planka.extension.comment.config.CommentProperties;
import dev.planka.extension.comment.model.CommentEntity;
import dev.planka.extension.comment.repository.CommentCardRefRepository;
import dev.planka.extension.comment.repository.CommentMentionRepository;
import dev.planka.extension.comment.repository.CommentRepository;
import dev.planka.infra.cache.card.CardCacheService;
import dev.planka.infra.cache.schema.query.BizRuleCacheQuery;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("评论服务测试")
class CommentServiceTest {

    @Mock
    private CommentRepository commentRepository;

    @Mock
    private CommentMentionRepository mentionRepository;

    @Mock
    private CommentCardRefRepository cardRefRepository;

    @Mock
    private CommentProperties commentProperties;

    @Mock
    private CardCacheService cardCacheService;

    @Mock
    private ObjectMapper objectMapper;

    @Mock
    private BizRuleCacheQuery bizRuleCacheQuery;

    @InjectMocks
    private CommentServiceImpl commentService;

    private static final String ORG_ID = "org-1";
    private static final String USER_ID = "user-1";
    private static final String CARD_ID = "card-1";
    private static final String CARD_TYPE_ID = "card-type-1";

    @BeforeEach
    void setUp() {
        // Mock cardCacheService to return empty map by default
        when(cardCacheService.getBasicInfoByIds(any())).thenReturn(Collections.emptyMap());
    }

    @Test
    @DisplayName("创建评论成功")
    void createComment_success() {
        // Given
        CreateCommentRequest request = new CreateCommentRequest(
                CARD_ID,
                CARD_TYPE_ID,
                null,
                null,
                "这是一条测试评论",
                List.of(),
                List.of(),
                new UserOperationSource()
        );

        when(commentRepository.insert(any(CommentEntity.class))).thenReturn(1);
        when(mentionRepository.selectList(any())).thenReturn(Collections.emptyList());
        when(cardRefRepository.selectList(any())).thenReturn(Collections.emptyList());
        when(commentRepository.selectList(any())).thenReturn(Collections.emptyList());

        // When
        CommentDTO result = commentService.createComment(ORG_ID, USER_ID, request);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.content()).isEqualTo("这是一条测试评论");
        assertThat(result.status()).isEqualTo("ACTIVE");
        verify(commentRepository).insert(any(CommentEntity.class));
    }

    @Test
    @DisplayName("创建回复评论成功")
    void createReplyComment_success() {
        // Given
        Long parentId = 100L;
        CommentEntity parentComment = createCommentEntity(parentId, null);
        when(commentRepository.selectById(parentId)).thenReturn(parentComment);

        CreateCommentRequest request = new CreateCommentRequest(
                CARD_ID,
                CARD_TYPE_ID,
                String.valueOf(parentId),
                "member-2",
                "这是一条回复",
                List.of(),
                List.of(),
                new UserOperationSource()
        );

        when(commentRepository.insert(any(CommentEntity.class))).thenReturn(1);
        when(mentionRepository.selectList(any())).thenReturn(Collections.emptyList());
        when(cardRefRepository.selectList(any())).thenReturn(Collections.emptyList());
        when(commentRepository.selectList(any())).thenReturn(Collections.emptyList());

        // When
        CommentDTO result = commentService.createComment(ORG_ID, USER_ID, request);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.rootId()).isEqualTo(parentId);
        assertThat(result.replyToMemberId()).isEqualTo("member-2");
    }

    @Test
    @DisplayName("撤回评论成功")
    void withdrawComment_success() {
        // Given
        Long commentId = 100L;
        CommentEntity comment = createCommentEntity(commentId, null);
        comment.setCreatedAt(LocalDateTime.now().minusSeconds(60)); // 1分钟前创建

        when(commentProperties.getWithdrawTimeLimit()).thenReturn(120);
        when(commentRepository.selectById(commentId)).thenReturn(comment);
        when(commentRepository.updateById(any(CommentEntity.class))).thenReturn(1);
        when(mentionRepository.selectList(any())).thenReturn(Collections.emptyList());
        when(cardRefRepository.selectList(any())).thenReturn(Collections.emptyList());
        when(commentRepository.selectList(any())).thenReturn(Collections.emptyList());

        // When
        CommentDTO result = commentService.withdrawComment(commentId, ORG_ID, USER_ID);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.status()).isEqualTo("WITHDRAWN");
    }

    @Test
    @DisplayName("撤回超时应抛出异常")
    void withdrawComment_timeExceeded_throwsException() {
        // Given
        Long commentId = 100L;
        CommentEntity comment = createCommentEntity(commentId, null);
        comment.setCreatedAt(LocalDateTime.now().minusMinutes(5)); // 5分钟前创建

        when(commentProperties.getWithdrawTimeLimit()).thenReturn(120);
        when(commentRepository.selectById(commentId)).thenReturn(comment);

        // When & Then
        assertThatThrownBy(() -> commentService.withdrawComment(commentId, ORG_ID, USER_ID))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("撤回时间限制");
    }

    @Test
    @DisplayName("非作者撤回应抛出异常")
    void withdrawComment_notAuthor_throwsException() {
        // Given
        Long commentId = 100L;
        CommentEntity comment = createCommentEntity(commentId, null);
        comment.setAuthorId("other-user");

        when(commentRepository.selectById(commentId)).thenReturn(comment);

        // When & Then
        assertThatThrownBy(() -> commentService.withdrawComment(commentId, ORG_ID, USER_ID))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("只能撤回自己的评论");
    }

    @Test
    @DisplayName("更新已撤回的评论成功")
    void updateComment_withdrawnComment_success() {
        // Given
        Long commentId = 100L;
        CommentEntity comment = createCommentEntity(commentId, null);
        comment.setStatus("WITHDRAWN");

        UpdateCommentRequest request = new UpdateCommentRequest(
                "更新后的评论内容",
                null,
                null
        );

        when(commentRepository.selectById(commentId)).thenReturn(comment);
        when(commentRepository.updateById(any(CommentEntity.class))).thenReturn(1);
        when(mentionRepository.delete(any())).thenReturn(0);
        when(cardRefRepository.delete(any())).thenReturn(0);
        when(mentionRepository.selectList(any())).thenReturn(Collections.emptyList());
        when(cardRefRepository.selectList(any())).thenReturn(Collections.emptyList());
        when(commentRepository.selectList(any())).thenReturn(Collections.emptyList());

        // When
        CommentDTO result = commentService.updateComment(commentId, ORG_ID, USER_ID, request);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.status()).isEqualTo("ACTIVE");
        assertThat(result.editCount()).isEqualTo(1);
    }

    @Test
    @DisplayName("更新未撤回的评论应抛出异常")
    void updateComment_activeComment_throwsException() {
        // Given
        Long commentId = 100L;
        CommentEntity comment = createCommentEntity(commentId, null);
        comment.setStatus("ACTIVE");

        UpdateCommentRequest request = new UpdateCommentRequest("内容", null, null);

        when(commentRepository.selectById(commentId)).thenReturn(comment);

        // When & Then
        assertThatThrownBy(() -> commentService.updateComment(commentId, ORG_ID, USER_ID, request))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("已撤回");
    }

    @Test
    @DisplayName("删除评论成功")
    void deleteComment_success() {
        // Given
        Long commentId = 100L;
        CommentEntity comment = createCommentEntity(commentId, null);

        when(commentRepository.selectById(commentId)).thenReturn(comment);
        when(commentRepository.updateById(any(CommentEntity.class))).thenReturn(1);

        // When
        commentService.deleteComment(commentId, ORG_ID, USER_ID);

        // Then
        verify(commentRepository).updateById(any(CommentEntity.class));
    }

    private CommentEntity createCommentEntity(Long id, Long parentId) {
        CommentEntity entity = new CommentEntity();
        entity.setId(id);
        entity.setOrgId(ORG_ID);
        entity.setCardId(CARD_ID);
        entity.setCardTypeId(CARD_TYPE_ID);
        entity.setParentId(parentId);
        entity.setRootId(parentId);
        entity.setContent("测试评论内容");
        entity.setStatus("ACTIVE");
        entity.setEditCount(0);
        entity.setAuthorId(USER_ID);
        entity.setCreatedAt(LocalDateTime.now());
        entity.setUpdatedAt(LocalDateTime.now());
        return entity;
    }
}
