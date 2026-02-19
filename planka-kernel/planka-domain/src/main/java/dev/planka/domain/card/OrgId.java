package dev.planka.domain.card;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

import java.util.Objects;

/**
 * 组织ID值对象
 * <p>
 * 序列化时只输出 value 值。
 */
public record OrgId(@JsonValue String value) {

    private static final long serialVersionUID = 1L;

    public OrgId {
        Objects.requireNonNull(value, "OrgId value cannot be null");
        if (value.isBlank()) {
            throw new IllegalArgumentException("OrgId value cannot be blank");
        }
    }

    @JsonCreator(mode = JsonCreator.Mode.DELEGATING)
    public static OrgId of(String value) {
        return new OrgId(value);
    }

    @Override
    public String toString() {
        return value;
    }
}
