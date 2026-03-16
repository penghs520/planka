package cn.planka.notification.plugin.email;

import cn.planka.notification.plugin.NotificationChannelContext;
import cn.planka.notification.plugin.NotificationRequest;
import cn.planka.notification.plugin.NotificationResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 邮件插件集成测试
 * <p>
 * 需要真实的 SMTP 服务器（如 Mailhog）
 * 默认禁用，手动启用后运行
 * </p>
 */
@Disabled("需要真实 SMTP 服务器，手动启用")
class EmailChannelIntegrationTest {

    private EmailChannel emailChannel;

    @BeforeEach
    void setUp() {
        emailChannel = new EmailChannel();

        // 配置测试 SMTP 服务器（Mailhog）
        NotificationChannelContext context = new NotificationChannelContext() {
            @Override
            public String getEnvironment() {
                return "test";
            }

            @Override
            public String getProperty(String key) {
                return switch (key) {
                    case "smtp.host" -> "localhost";
                    case "smtp.port" -> "1025";  // Mailhog SMTP 端口
                    case "sender.email" -> "test@example.com";
                    case "sender.password" -> "";
                    case "encryption.type" -> "NONE";
                    case "sender.name" -> "测试系统";
                    default -> null;
                };
            }

            @Override
            public String getProperty(String key, String defaultValue) {
                String value = getProperty(key);
                return value != null ? value : defaultValue;
            }
        };

        emailChannel.initialize(context);
    }

    @Test
    void send_shouldSendEmailSuccessfully() {
        // Given
        NotificationRequest request = NotificationRequest.builder()
                .orgId("org1")
                .title("集成测试邮件")
                .content("这是一封集成测试邮件")
                .richContent("<h1>集成测试邮件</h1><p>这是一封集成测试邮件</p>")
                .recipients(List.of(
                        NotificationRequest.RecipientInfo.builder()
                                .userId("user1")
                                .name("测试用户")
                                .email("test@example.com")
                                .build()
                ))
                .build();

        // When
        NotificationResult result = emailChannel.send(request);

        // Then
        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getChannelId()).isEqualTo("email");
        assertThat(result.getRecipientResults()).hasSize(1);
        assertThat(result.getRecipientResults().get(0).isSuccess()).isTrue();
    }

    @Test
    void send_shouldHandleMultipleRecipients() {
        // Given
        NotificationRequest request = NotificationRequest.builder()
                .orgId("org1")
                .title("批量测试邮件")
                .content("这是一封批量测试邮件")
                .recipients(List.of(
                        NotificationRequest.RecipientInfo.builder()
                                .userId("user1")
                                .email("test1@example.com")
                                .build(),
                        NotificationRequest.RecipientInfo.builder()
                                .userId("user2")
                                .email("test2@example.com")
                                .build(),
                        NotificationRequest.RecipientInfo.builder()
                                .userId("user3")
                                .email("test3@example.com")
                                .build()
                ))
                .build();

        // When
        NotificationResult result = emailChannel.send(request);

        // Then
        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getRecipientResults()).hasSize(3);
        assertThat(result.getRecipientResults()).allMatch(NotificationResult.RecipientResult::isSuccess);
    }

    @Test
    void send_shouldHandleInvalidEmail() {
        // Given
        NotificationRequest request = NotificationRequest.builder()
                .orgId("org1")
                .title("测试邮件")
                .content("测试内容")
                .recipients(List.of(
                        NotificationRequest.RecipientInfo.builder()
                                .userId("user1")
                                .email("")  // 空邮箱
                                .build()
                ))
                .build();

        // When
        NotificationResult result = emailChannel.send(request);

        // Then
        assertThat(result.isSuccess()).isFalse();
        assertThat(result.getRecipientResults()).hasSize(1);
        assertThat(result.getRecipientResults().get(0).isSuccess()).isFalse();
        assertThat(result.getRecipientResults().get(0).getErrorMessage()).contains("邮箱地址为空");
    }
}
