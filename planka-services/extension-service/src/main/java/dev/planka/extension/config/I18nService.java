package dev.planka.extension.config;

import lombok.RequiredArgsConstructor;
import org.springframework.context.MessageSource;
import org.springframework.context.NoSuchMessageException;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.stereotype.Component;

import java.util.Locale;

/**
 * 国际化服务
 * <p>
 * 提供便捷的消息获取方法，支持根据当前请求的语言环境获取本地化消息
 */
@Component
@RequiredArgsConstructor
public class I18nService {

    private final MessageSource messageSource;

    /**
     * 获取消息（使用当前请求的语言环境）
     *
     * @param code 消息码
     * @param args 参数
     * @return 本地化消息，如果找不到则返回消息码本身
     */
    public String getMessage(String code, Object... args) {
        return getMessage(code, LocaleContextHolder.getLocale(), args);
    }

    /**
     * 获取消息（指定语言环境）
     *
     * @param code   消息码
     * @param locale 语言环境
     * @param args   参数
     * @return 本地化消息，如果找不到则返回消息码本身
     */
    public String getMessage(String code, Locale locale, Object... args) {
        try {
            return messageSource.getMessage(code, args, locale);
        } catch (NoSuchMessageException e) {
            return code;
        }
    }

}
