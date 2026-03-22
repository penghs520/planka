package cn.planka.domain.notification;

import cn.planka.domain.card.CardTypeId;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;

/**
 * __PLANKA_EINST__定义参数
 * <p>
 * 需要选择具体的 CardTypeId，通知模板绑定到特定__PLANKA_EINST__。
 */
@Getter
@JsonIgnoreProperties(ignoreUnknown = true)
public final class CardTypeDefinitionParameter implements DefinitionParameter {

    public static final String TYPE = "CARD_TYPE";

    /**
     * __PLANKA_EINST__ID
     */
    @JsonProperty("cardTypeId")
    private final CardTypeId cardTypeId;

    /**
     * __PLANKA_EINST__名称（用于显示）
     */
    @JsonProperty("cardTypeName")
    private String cardTypeName;

    @JsonCreator
    public CardTypeDefinitionParameter(
            @JsonProperty("cardTypeId") CardTypeId cardTypeId) {
        this.cardTypeId = cardTypeId;
    }

    public CardTypeDefinitionParameter(CardTypeId cardTypeId, String cardTypeName) {
        this.cardTypeId = cardTypeId;
        this.cardTypeName = cardTypeName;
    }

    @Override
    public DefinitionParameterType getType() {
        return DefinitionParameterType.CARD_TYPE;
    }

    @Override
    public String getDisplayName() {
        return cardTypeName != null ? cardTypeName : (cardTypeId != null ? cardTypeId.value() : null);
    }

    public void setCardTypeName(String cardTypeName) {
        this.cardTypeName = cardTypeName;
    }

    /**
     * 工厂方法
     */
    public static CardTypeDefinitionParameter of(String cardTypeId) {
        return new CardTypeDefinitionParameter(new CardTypeId(cardTypeId));
    }

    public static CardTypeDefinitionParameter of(CardTypeId cardTypeId) {
        return new CardTypeDefinitionParameter(cardTypeId);
    }

    public static CardTypeDefinitionParameter of(CardTypeId cardTypeId, String cardTypeName) {
        return new CardTypeDefinitionParameter(cardTypeId, cardTypeName);
    }
}
