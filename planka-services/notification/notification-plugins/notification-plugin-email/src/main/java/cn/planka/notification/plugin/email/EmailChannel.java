package cn.planka.notification.plugin.email;

import cn.planka.notification.plugin.*;
import cn.planka.notification.plugin.*;
import jakarta.mail.internet.MimeMessage;
import lombok.extern.slf4j.Slf4j;
import org.pf4j.Extension;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.mail.javamail.MimeMessageHelper;

import java.util.*;

/**
 * 邮件通知渠道
 * <p>
 * 通过 SMTP 发送邮件通知
 * 支持纯文本和 HTML 富文本
 * </p>
 *
 * @author Planka
 * @since 2.0.0
 */
@Slf4j
@Extension
public class EmailChannel implements NotificationChannelPlugin {

    public static final String CHANNEL_ID = "email";
    private static final int MAX_RETRY = 3;
    private static final long RETRY_DELAY_MS = 1000;

    private final NotificationChannelDef def;
    private JavaMailSender mailSender;
    private String senderEmail;
    private String senderName;

    public EmailChannel() {
        this.def = NotificationChannelDef.builder()
                .id(CHANNEL_ID)
                .name("邮件通知")
                .description("通过 SMTP 发送邮件通知")
                .version(version())
                .provider("Planka")
                .supportsRichContent(true)
                .supportsAttachment(true)
                .configFields(buildConfigFields())
                .build();
    }

    @Override
    public String channelId() {
        return CHANNEL_ID;
    }

    @Override
    public String name() {
        return "邮件通知";
    }

    @Override
    public NotificationChannelDef def() {
        return def;
    }

    @Override
    public void initialize(NotificationChannelContext context) {
        // 从上下文获取配置
        String smtpHost = context.getProperty("smtp.host");
        int smtpPort = Integer.parseInt(context.getProperty("smtp.port", "25"));
        this.senderEmail = context.getProperty("sender.email");
        String senderPassword = context.getProperty("sender.password");
        String encryptionType = context.getProperty("encryption.type", "TLS");
        this.senderName = context.getProperty("sender.name", "系统通知");

        // 创建 JavaMailSender
        JavaMailSenderImpl sender = new JavaMailSenderImpl();
        sender.setHost(smtpHost);
        sender.setPort(smtpPort);
        sender.setUsername(senderEmail);
        sender.setPassword(senderPassword);

        // 配置 SMTP 属性
        Properties props = sender.getJavaMailProperties();
        props.put("mail.transport.protocol", "smtp");
        props.put("mail.smtp.auth", "true");
        props.put("mail.smtp.timeout", "30000");
        props.put("mail.smtp.connectiontimeout", "30000");

        // 配置加密方式
        if ("TLS".equalsIgnoreCase(encryptionType)) {
            props.put("mail.smtp.starttls.enable", "true");
            props.put("mail.smtp.starttls.required", "true");
        } else if ("SSL".equalsIgnoreCase(encryptionType)) {
            props.put("mail.smtp.ssl.enable", "true");
        }

        this.mailSender = sender;
        log.info("邮件渠道初始化成功: host={}, port={}, sender={}", smtpHost, smtpPort, senderEmail);
    }

    @Override
    public NotificationResult send(NotificationRequest request) {
        if (mailSender == null) {
            return NotificationResult.failed(CHANNEL_ID, "NOT_INITIALIZED", "邮件渠道未初始化");
        }

        try {
            String messageId = "email_" + UUID.randomUUID().toString().replace("-", "");
            List<NotificationResult.RecipientResult> recipientResults = new ArrayList<>();

            // 为每个接收者发送邮件
            for (NotificationRequest.RecipientInfo recipient : request.getRecipients()) {
                if (recipient.getEmail() == null || recipient.getEmail().isBlank()) {
                    recipientResults.add(NotificationResult.RecipientResult.builder()
                            .userId(recipient.getUserId())
                            .success(false)
                            .errorMessage("邮箱地址为空")
                            .build());
                    continue;
                }

                NotificationResult.RecipientResult result = sendToRecipient(request, recipient);
                recipientResults.add(result);
            }

            boolean allSuccess = recipientResults.stream().allMatch(NotificationResult.RecipientResult::isSuccess);
            if (allSuccess) {
                log.info("邮件发送成功: messageId={}, recipients={}", messageId, recipientResults.size());
                return NotificationResult.success(CHANNEL_ID, messageId, recipientResults);
            } else {
                return NotificationResult.partial(CHANNEL_ID, messageId, recipientResults);
            }
        } catch (Exception e) {
            log.error("邮件发送失败: error={}", e.getMessage(), e);
            return NotificationResult.failed(CHANNEL_ID, "SEND_ERROR", e.getMessage());
        }
    }

    @Override
    public List<NotificationResult> sendBatch(List<NotificationRequest> requests) {
        return requests.stream()
                .map(this::send)
                .toList();
    }

    @Override
    public boolean supportsRichContent() {
        return true;
    }

    @Override
    public boolean supportsAttachment() {
        return true;
    }

    @Override
    public boolean requiresExternalUserMapping() {
        return false;
    }

    /**
     * 发送邮件给单个接收者（带重试）
     */
    private NotificationResult.RecipientResult sendToRecipient(
            NotificationRequest request,
            NotificationRequest.RecipientInfo recipient) {

        Exception lastException = null;
        for (int attempt = 1; attempt <= MAX_RETRY; attempt++) {
            try {
                sendEmail(recipient.getEmail(), request.getTitle(),
                         request.getContent(), request.getRichContent());

                return NotificationResult.RecipientResult.builder()
                        .userId(recipient.getUserId())
                        .success(true)
                        .build();
            } catch (Exception e) {
                lastException = e;
                log.warn("邮件发送失败，尝试重试: attempt={}/{}, email={}, error={}",
                        attempt, MAX_RETRY, recipient.getEmail(), e.getMessage());

                if (attempt < MAX_RETRY) {
                    try {
                        Thread.sleep(RETRY_DELAY_MS);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        break;
                    }
                }
            }
        }

        return NotificationResult.RecipientResult.builder()
                .userId(recipient.getUserId())
                .success(false)
                .errorMessage(lastException != null ? lastException.getMessage() : "未知错误")
                .build();
    }

    /**
     * 发送邮件
     */
    private void sendEmail(String to, String subject, String textContent, String htmlContent) throws Exception {
        MimeMessage message = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

        helper.setFrom(senderEmail, senderName);
        helper.setTo(to);
        helper.setSubject(subject);
        helper.setSentDate(new Date());

        // 优先使用 HTML 内容，否则使用纯文本
        if (htmlContent != null && !htmlContent.isBlank()) {
            helper.setText(textContent, htmlContent);
        } else {
            helper.setText(textContent, false);
        }

        mailSender.send(message);
    }

    /**
     * 构建配置字段定义
     */
    private List<NotificationChannelDef.ConfigField> buildConfigFields() {
        return List.of(
                NotificationChannelDef.ConfigField.builder()
                        .key("smtp.host")
                        .label("SMTP 服务器地址")
                        .type("text")
                        .required(true)
                        .description("SMTP 服务器地址，如 smtp.example.com")
                        .placeholder("smtp.example.com")
                        .build(),
                NotificationChannelDef.ConfigField.builder()
                        .key("smtp.port")
                        .label("SMTP 端口")
                        .type("number")
                        .required(true)
                        .defaultValue("25")
                        .description("SMTP 服务器端口，默认 25")
                        .build(),
                NotificationChannelDef.ConfigField.builder()
                        .key("sender.email")
                        .label("发件人邮箱")
                        .type("text")
                        .required(true)
                        .description("发件人邮箱地址")
                        .placeholder("noreply@example.com")
                        .build(),
                NotificationChannelDef.ConfigField.builder()
                        .key("sender.password")
                        .label("发件人密码")
                        .type("password")
                        .required(true)
                        .description("发件人邮箱密码或授权码")
                        .build(),
                NotificationChannelDef.ConfigField.builder()
                        .key("encryption.type")
                        .label("加密方式")
                        .type("select")
                        .required(true)
                        .defaultValue("TLS")
                        .description("SMTP 加密方式")
                        .options(List.of(
                                NotificationChannelDef.ConfigField.SelectOption.builder().value("TLS").label("TLS").build(),
                                NotificationChannelDef.ConfigField.SelectOption.builder().value("SSL").label("SSL").build(),
                                NotificationChannelDef.ConfigField.SelectOption.builder().value("NONE").label("无").build()
                        ))
                        .build(),
                NotificationChannelDef.ConfigField.builder()
                        .key("sender.name")
                        .label("发件人名称")
                        .type("text")
                        .required(false)
                        .defaultValue("系统通知")
                        .description("发件人显示名称")
                        .placeholder("系统通知")
                        .build()
        );
    }
}

