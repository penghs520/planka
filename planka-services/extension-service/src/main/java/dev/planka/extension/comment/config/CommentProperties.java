package dev.planka.extension.comment.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * 评论服务配置
 */
@Configuration
@ConfigurationProperties(prefix = "comment")
public class CommentProperties {

    /**
     * 撤回时间限制（秒），默认 120 秒（2分钟）
     */
    private int withdrawTimeLimit = 120;

    public int getWithdrawTimeLimit() {
        return withdrawTimeLimit;
    }

    public void setWithdrawTimeLimit(int withdrawTimeLimit) {
        this.withdrawTimeLimit = withdrawTimeLimit;
    }
}
