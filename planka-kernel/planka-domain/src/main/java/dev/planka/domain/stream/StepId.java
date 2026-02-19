package dev.planka.domain.stream;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

import java.util.Objects;

/**
 * 价值流阶段ID值对象
 */
public record StepId(@JsonValue String value) {

    public StepId {
        Objects.requireNonNull(value, "StepId value cannot be null");
        if (value.isBlank()) {
            throw new IllegalArgumentException("StepId value cannot be blank");
        }
    }

    @JsonCreator(mode = JsonCreator.Mode.DELEGATING)
    public static StepId of(String value) {
        return new StepId(value);
    }

    @Override
    public String toString() {
        return value;
    }
}
