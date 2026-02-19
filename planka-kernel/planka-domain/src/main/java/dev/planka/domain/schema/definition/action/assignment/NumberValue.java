package dev.planka.domain.schema.definition.action.assignment;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

/**
 * 数字值
 */
@Getter
@Setter
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public final class NumberValue implements FixedValue {

    /**
     * 数字值
     */
    @JsonProperty("number")
    private BigDecimal number;

    @Override
    public String getValueType() {
        return "NUMBER";
    }
}
