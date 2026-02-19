package dev.planka.domain.card;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;

/**
 * 卡片描述
 * <p>
 * 包含卡片的详细描述信息。
 * 未来可以扩展权限控制，决定谁可以读写描述。
 */
@Getter
@JsonIgnoreProperties(ignoreUnknown = true)
public class CardDescription {

    /**
     * 描述内容
     */
    private final String value;

    @JsonCreator
    public CardDescription(@JsonProperty("value") String value) {
        this.value = value;
    }

    /**
     * 创建描述的便捷方法
     */
    public static CardDescription of(String value) {
        return new CardDescription(value);
    }

    /**
     * 判断描述是否为空
     */
    public boolean isEmpty() {
        return value == null || value.isBlank();
    }

    /**
     * 获取描述内容，如果为 null 则返回空字符串
     */
    public String getValueOrEmpty() {
        return value != null ? value : "";
    }
}
