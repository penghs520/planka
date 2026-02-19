package dev.planka.domain.history.source;

import dev.planka.domain.history.OperationSource;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;

/**
 * 数据导入来源
 */
@Getter
@JsonIgnoreProperties(ignoreUnknown = true)
public final class ImportOperationSource implements OperationSource {

    public static final String TYPE = "IMPORT";
    private static final String MESSAGE_KEY = "history.source.import";

    /**
     * 导入批次ID
     */
    private final String batchId;

    /**
     * 导入文件名
     */
    private final String fileName;

    @JsonCreator
    public ImportOperationSource(
            @JsonProperty("batchId") String batchId,
            @JsonProperty("fileName") String fileName) {
        this.batchId = batchId;
        this.fileName = fileName;
    }

    @Override
    public String getType() {
        return TYPE;
    }

    @Override
    public String getMessageKey() {
        return MESSAGE_KEY;
    }
}
