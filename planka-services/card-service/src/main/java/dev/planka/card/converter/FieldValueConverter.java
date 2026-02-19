package dev.planka.card.converter;

import dev.planka.domain.field.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 字段值转换器
 * 使用 FieldValue 子类型的 instanceof 匹配来完成双向转换
 */
public class FieldValueConverter {

    private static final Logger logger = LoggerFactory.getLogger(FieldValueConverter.class);

    private FieldValueConverter() {
    }

    // ==================== Domain -> Proto ====================

    /**
     * 将 domain FieldValue Map 转换为 proto FieldValue Map
     */
    public static Map<String, planka.graph.driver.proto.field.FieldValue> toProtoMap(
            Map<String, FieldValue<?>> fieldValues) {
        if (fieldValues == null || fieldValues.isEmpty()) {
            return new HashMap<>();
        }

        Map<String, planka.graph.driver.proto.field.FieldValue> result = new HashMap<>();
        for (Map.Entry<String, FieldValue<?>> entry : fieldValues.entrySet()) {
            planka.graph.driver.proto.field.FieldValue protoValue = toProto(entry.getValue());
            if (protoValue != null) {
                result.put(entry.getKey(), protoValue);
            }
        }
        return result;
    }

    /**
     * 将 domain FieldValue 转换为 proto FieldValue
     * 使用 instanceof 进行类型匹配
     */
    public static planka.graph.driver.proto.field.FieldValue toProto(FieldValue<?> fieldValue) {
        if (fieldValue == null) {
            return null;
        }

        planka.graph.driver.proto.field.FieldValue.Builder builder = planka.graph.driver.proto.field.FieldValue.newBuilder();
        builder.setFieldId(fieldValue.getFieldId());

        // 使用 instanceof 进行类型匹配
        if (fieldValue instanceof TextFieldValue textValue) {
            planka.graph.driver.proto.field.TextFieldValue.Builder textBuilder = planka.graph.driver.proto.field.TextFieldValue
                    .newBuilder();
            if (textValue.getValue() != null) {
                textBuilder.setValue(textValue.getValue());
            }
            if (textValue.getMaxStringLength() != null) {
                textBuilder.setMaxStringLength(textValue.getMaxStringLength());
            }
            builder.setTextField(textBuilder.build());
        } else if (fieldValue instanceof NumberFieldValue numberValue) {
            planka.graph.driver.proto.field.NumberFieldValue.Builder numberBuilder = planka.graph.driver.proto.field.NumberFieldValue
                    .newBuilder();
            if (numberValue.getValue() != null) {
                numberBuilder.setValue(numberValue.getValue());
            }
            builder.setNumberField(numberBuilder.build());
        } else if (fieldValue instanceof DateFieldValue dateValue) {
            planka.graph.driver.proto.field.DateFieldValue.Builder dateBuilder = planka.graph.driver.proto.field.DateFieldValue
                    .newBuilder();
            if (dateValue.getValue() != null) {
                dateBuilder.setValue(dateValue.getValue());
            }
            builder.setDateField(dateBuilder.build());
        } else if (fieldValue instanceof EnumFieldValue enumValue) {
            planka.graph.driver.proto.field.EnumFieldValue.Builder enumBuilder = planka.graph.driver.proto.field.EnumFieldValue
                    .newBuilder();
            if (enumValue.getValue() != null) {
                enumBuilder.addAllValue(enumValue.getValue());
            }
            builder.setEnumField(enumBuilder.build());
        } else if (fieldValue instanceof WebLinkFieldValue webLinkValue) {
            planka.graph.driver.proto.field.WebLinkFieldValue.Builder webLinkBuilder = planka.graph.driver.proto.field.WebLinkFieldValue
                    .newBuilder();
            if (webLinkValue.getValue() != null) {
                Url url = webLinkValue.getValue();
                webLinkBuilder.setHref(url.url() != null ? url.url() : "");
                webLinkBuilder.setName(url.displayText() != null ? url.displayText() : "");
            }
            builder.setWebLinkField(webLinkBuilder.build());
        } else if (fieldValue instanceof AttachmentFieldValue attachmentValue) {
            planka.graph.driver.proto.field.AttachmentFieldValue.Builder attachBuilder = planka.graph.driver.proto.field.AttachmentFieldValue
                    .newBuilder();
            if (attachmentValue.getValue() != null) {
                for (Attachment att : attachmentValue.getValue()) {
                    attachBuilder.addValue(planka.graph.driver.proto.field.AttachmentItem.newBuilder()
                            .setId(att.id() != null ? att.id() : "")
                            .setName(att.name() != null ? att.name() : "")
                            .setSize(att.size())
                            .build());
                }
            }
            builder.setAttachmentField(attachBuilder.build());
        } else if (fieldValue instanceof StructureFieldValue) {
            // StructureFieldValue 在 proto 中没有对应类型，无需转换
            return null;
        } else {
            logger.warn("未知的 FieldValue 类型: {}", fieldValue.getClass().getName());
            return null;
        }

        return builder.build();
    }

    // ==================== Proto -> Domain ====================

    /**
     * 将 proto FieldValue Map 转换为 domain FieldValue Map
     */
    public static Map<String, FieldValue<?>> fromProtoMap(
            Map<String, planka.graph.driver.proto.field.FieldValue> protoFieldValues) {
        if (protoFieldValues == null || protoFieldValues.isEmpty()) {
            return new HashMap<>();
        }

        Map<String, FieldValue<?>> result = new HashMap<>();
        for (Map.Entry<String, planka.graph.driver.proto.field.FieldValue> entry : protoFieldValues.entrySet()) {
            FieldValue<?> domainValue = fromProto(entry.getValue());
            if (domainValue != null) {
                result.put(entry.getKey(), domainValue);
            }
        }
        return result;
    }

    /**
     * 将 proto FieldValue 转换为 domain FieldValue
     */
    public static FieldValue<?> fromProto(planka.graph.driver.proto.field.FieldValue protoValue) {
        if (protoValue == null) {
            return null;
        }

        String fieldId = protoValue.getFieldId();

        // 根据 proto 的 oneof 类型判断并创建对应的 domain 类型
        if (protoValue.hasTextField()) {
            planka.graph.driver.proto.field.TextFieldValue textField = protoValue.getTextField();
            Long maxLength = textField.getMaxStringLength() > 0 ? textField.getMaxStringLength() : null;
            return new TextFieldValue(fieldId, textField.getValue(), maxLength);
        }

        if (protoValue.hasNumberField()) {
            planka.graph.driver.proto.field.NumberFieldValue numberField = protoValue.getNumberField();
            return new NumberFieldValue(fieldId, numberField.getValue());
        }

        if (protoValue.hasDateField()) {
            planka.graph.driver.proto.field.DateFieldValue dateField = protoValue.getDateField();
            return new DateFieldValue(fieldId, dateField.getValue());
        }

        if (protoValue.hasEnumField()) {
            planka.graph.driver.proto.field.EnumFieldValue enumField = protoValue.getEnumField();
            List<String> optionIds = new ArrayList<>(enumField.getValueList());
            return new EnumFieldValue(fieldId, optionIds);
        }

        if (protoValue.hasWebLinkField()) {
            planka.graph.driver.proto.field.WebLinkFieldValue webLinkField = protoValue.getWebLinkField();
            return new WebLinkFieldValue(fieldId,
                    new Url(webLinkField.getHref(), webLinkField.getName()));
        }

        if (protoValue.hasAttachmentField()) {
            planka.graph.driver.proto.field.AttachmentFieldValue attachField = protoValue.getAttachmentField();
            List<Attachment> attachments = attachField.getValueList().stream()
                    .map(item -> new Attachment(
                            item.getId(),
                            item.getName(),
                            null, // url not in proto
                            item.getSize(),
                            null)) // contentType not in proto
                    .collect(Collectors.toList());
            return new AttachmentFieldValue(fieldId, attachments, true);
        }

        if (protoValue.hasDeriveField()) {
            // DeriveFieldValue 用于关联关系上的衍生属性，暂不支持
            logger.debug("DeriveFieldValue 类型暂不支持: {}", fieldId);
            return null;
        }

        logger.warn("未知的 proto FieldValue 类型: {}", fieldId);
        return null;
    }
}
