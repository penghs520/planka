package dev.planka.domain.field;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * 架构节点（链表结构，支持层级路径）
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public final class StructureItem {
    private final String id;
    private final String name;
    private StructureItem next;

    public StructureItem(String id, String name) {
        this(id, name, null);
    }

    @JsonCreator
    public StructureItem(
            @JsonProperty("id") String id,
            @JsonProperty("name") String name,
            @JsonProperty("next") StructureItem next) {
        this.id = id;
        this.name = name;
        this.next = next;
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public StructureItem getNext() {
        return next;
    }

    public void setNext(StructureItem next) {
        this.next = next;
    }

    /**
     * 获取链表最后一个节点
     */
    public StructureItem last() {
        StructureItem last = this;
        while (last.getNext() != null) {
            last = last.getNext();
        }
        return last;
    }

    /**
     * 在链表末尾添加节点
     */
    public StructureItem addNext(StructureItem nextToAdd) {
        last().setNext(nextToAdd);
        return this;
    }
}
