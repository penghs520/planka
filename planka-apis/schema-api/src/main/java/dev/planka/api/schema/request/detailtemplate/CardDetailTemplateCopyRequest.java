package dev.planka.api.schema.request.detailtemplate;

import dev.planka.common.util.AssertUtils;

public record CardDetailTemplateCopyRequest(String newName) {

    public CardDetailTemplateCopyRequest(String newName) {
        this.newName = AssertUtils.requireNotBlank(newName, "newName can't be blank");
    }
}
