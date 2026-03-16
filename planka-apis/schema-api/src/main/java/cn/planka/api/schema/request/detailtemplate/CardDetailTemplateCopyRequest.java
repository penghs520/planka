package cn.planka.api.schema.request.detailtemplate;

import cn.planka.common.util.AssertUtils;

public record CardDetailTemplateCopyRequest(String newName) {

    public CardDetailTemplateCopyRequest(String newName) {
        this.newName = AssertUtils.requireNotBlank(newName, "newName can't be blank");
    }
}
