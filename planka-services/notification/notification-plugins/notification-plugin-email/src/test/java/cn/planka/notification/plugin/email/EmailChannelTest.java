package cn.planka.notification.plugin.email;

import cn.planka.notification.plugin.NotificationChannelContext;
import cn.planka.notification.plugin.NotificationRequest;
import cn.planka.notification.plugin.NotificationResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EmailChannelTest {

    @Mock
    private NotificationChannelContext context;

    private EmailChannel emailChannel;

    @BeforeEach
    void setUp() {
        emailChannel = new EmailChannel();
    }

    @Test
    void channelId_shouldReturnEmail() {
        assertThat(emailChannel.channelId()).isEqualTo("email");
    }

    @Test
    void name_shouldReturnEmailNotification() {
        assertThat(emailChannel.name()).isEqualTo("邮件通知");
    }

    @Test
    void def_shouldReturnChannelDefinition() {
        var def = emailChannel.def();

        assertThat(def.getId()).isEqualTo("email");
        assertThat(def.getName()).isEqualTo("邮件通知");
        assertThat(def.getProvider()).isEqualTo("Planka");
        assertThat(def.isSupportsRichContent()).isTrue();
        assertThat(def.isSupportsAttachment()).isTrue();
        assertThat(def.getConfigFields()).hasSize(6);
    }

    @Test
    void supportsRichContent_shouldReturnTrue() {
        assertThat(emailChannel.supportsRichContent()).isTrue();
    }

    @Test
    void supportsAttachment_shouldReturnTrue() {
        assertThat(emailChannel.supportsAttachment()).isTrue();
    }

    @Test
    void requiresExternalUserMapping_shouldReturnFalse() {
        assertThat(emailChannel.requiresExternalUserMapping()).isFalse();
    }

    @Test
    void send_shouldReturnFailedResult_whenNotInitialized() {
        // Given
        NotificationRequest request = NotificationRequest.builder()
                .orgId("org1")
                .title("测试标题")
                .content("测试内容")
                .recipients(List.of(
                        NotificationRequest.RecipientInfo.builder()
                                .userId("user1")
                                .email("test@example.com")
                                .build()
                ))
                .build();

        // When
        NotificationResult result = emailChannel.send(request);

        // Then
        assertThat(result.isSuccess()).isFalse();
        assertThat(result.getErrorCode()).isEqualTo("NOT_INITIALIZED");
    }

    @Test
    void initialize_shouldConfigureMailSender() {
        // Given
        when(context.getProperty("smtp.host")).thenReturn("smtp.example.com");
        when(context.getProperty("smtp.port", "25")).thenReturn("587");
        when(context.getProperty("sender.email")).thenReturn("noreply@example.com");
        when(context.getProperty("sender.password")).thenReturn("password");
        when(context.getProperty("encryption.type", "TLS")).thenReturn("TLS");
        when(context.getProperty("sender.name", "系统通知")).thenReturn("测试系统");

        // When
        emailChannel.initialize(context);

        // Then - 验证初始化成功（通过发送测试，但会失败因为没有真实 SMTP 服务器）
        NotificationRequest request = NotificationRequest.builder()
                .orgId("org1")
                .title("测试标题")
                .content("测试内容")
                .recipients(List.of(
                        NotificationRequest.RecipientInfo.builder()
                                .userId("user1")
                                .email("test@example.com")
                                .build()
                ))
                .build();

        NotificationResult result = emailChannel.send(request);
        // 预期失败（因为没有真实 SMTP 服务器），但不是 NOT_INITIALIZED 错误
        assertThat(result.getErrorCode()).isNotEqualTo("NOT_INITIALIZED");
    }

    @Test
    void configFields_shouldContainRequiredFields() {
        var configFields = emailChannel.def().getConfigFields();

        assertThat(configFields).hasSize(6);
        assertThat(configFields).extracting("key").containsExactlyInAnyOrder(
                "smtp.host",
                "smtp.port",
                "sender.email",
                "sender.password",
                "encryption.type",
                "sender.name"
        );
    }
}
