<script setup lang="ts">
import { SchemaSubType } from '@/types/schema'

defineProps<{
  schemaSubType: SchemaSubType
  /** 仅 LINK：对侧多选入口时为 true */
  linkTargetMulti?: boolean
  /** 仅 ENUM：多选枚举入口时为 true */
  enumMultiSelect?: boolean
}>()
</script>

<template>
  <div class="ft-preview">
    <!-- 单行文本：输入框样式 -->
    <div v-if="schemaSubType === SchemaSubType.TEXT_FIELD" class="mock mock-input-line">
      <div class="mock-field" />
    </div>

    <!-- 多行文本 -->
    <div v-else-if="schemaSubType === SchemaSubType.MULTI_LINE_TEXT_FIELD" class="mock mock-textarea">
      <div class="mock-line" />
      <div class="mock-line mock-line--short" />
      <div class="mock-line mock-line--mid" />
    </div>

    <!-- Markdown：简易工具条 + 编辑区 -->
    <div v-else-if="schemaSubType === SchemaSubType.MARKDOWN_FIELD" class="mock mock-md">
      <div class="mock-md-bar">
        <span class="mock-md-pill" />
        <span class="mock-md-pill" />
        <span class="mock-md-pill mock-md-pill--wide" />
      </div>
      <div class="mock-md-body">
        <div class="mock-line" />
        <div class="mock-line mock-line--short" />
      </div>
    </div>

    <!-- 枚举：单选为单标签下拉；多选为多个标签 -->
    <div
      v-else-if="schemaSubType === SchemaSubType.ENUM_FIELD"
      class="mock mock-select"
      :class="{ 'mock-select--multi': enumMultiSelect }"
    >
      <div class="mock-select-inner">
        <div class="mock-select-pills">
          <template v-if="enumMultiSelect">
            <span class="mock-pill mock-pill--sm" />
            <span class="mock-pill mock-pill--sm" />
            <span class="mock-pill mock-pill--sm mock-pill--narrow" />
          </template>
          <span v-else class="mock-pill" />
        </div>
        <span class="mock-chevron" />
      </div>
    </div>

    <!-- 级联单选：树形下拉（触发器 + 展开面板内树） -->
    <div v-else-if="schemaSubType === SchemaSubType.CASCADE_FIELD" class="mock mock-tree-dd">
      <div class="mock-tree-trigger">
        <span class="mock-tree-trigger-ph" />
        <span class="mock-tree-chevron" />
      </div>
      <div class="mock-tree-panel">
        <div class="mock-tree-line">
          <span class="mock-tree-twist mock-tree-twist--open" />
          <span class="mock-tree-bar" />
        </div>
        <div class="mock-tree-nest">
          <div class="mock-tree-line">
            <span class="mock-tree-twist mock-tree-twist--open" />
            <span class="mock-tree-bar mock-tree-bar--mid" />
          </div>
          <div class="mock-tree-nest mock-tree-nest--deep">
            <div class="mock-tree-line">
              <span class="mock-tree-twist mock-tree-twist--dot" />
              <span class="mock-tree-bar mock-tree-bar--active" />
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- 关联类型：双卡片 + 连线；多选入口展示多个标签块 -->
    <div
      v-else-if="schemaSubType === SchemaSubType.LINK_FIELD"
      class="mock mock-link"
      :class="{ 'mock-link--multi': linkTargetMulti }"
    >
      <div class="mock-link-row">
        <div class="mock-link-card">
          <span class="mock-link-card-bar" />
        </div>
        <span class="mock-link-join" />
        <div class="mock-link-card mock-link-card--to">
          <span class="mock-link-card-bar mock-link-card-bar--short" />
        </div>
      </div>
      <div class="mock-link-tags">
        <template v-if="linkTargetMulti">
          <span class="mock-link-tag-pill mock-link-tag-pill--sm" />
          <span class="mock-link-tag-pill mock-link-tag-pill--sm" />
          <span class="mock-link-tag-pill mock-link-tag-pill--sm" />
        </template>
        <span v-else class="mock-link-tag-pill" />
      </div>
    </div>

    <!-- 日期 -->
    <div v-else-if="schemaSubType === SchemaSubType.DATE_FIELD" class="mock mock-date">
      <div class="mock-date-input">
        <span class="mock-date-text" />
        <span class="mock-cal" />
      </div>
    </div>

    <!-- 数字 -->
    <div v-else-if="schemaSubType === SchemaSubType.NUMBER_FIELD" class="mock mock-number">
      <div class="mock-field mock-field--numeric">
        <span class="mock-num-seg" />
        <span class="mock-num-seg mock-num-seg--dot" />
      </div>
    </div>

    <!-- 附件 -->
    <div v-else-if="schemaSubType === SchemaSubType.ATTACHMENT_FIELD" class="mock mock-file">
      <div class="mock-drop">
        <span class="mock-clip" />
        <span class="mock-file-name" />
      </div>
    </div>

    <!-- 网址 -->
    <div v-else-if="schemaSubType === SchemaSubType.WEB_URL_FIELD" class="mock mock-url">
      <div class="mock-url-row">
        <span class="mock-link-ico" />
        <div class="mock-field mock-field--url" />
      </div>
    </div>
  </div>
</template>

<style scoped>
.ft-preview {
  width: 100%;
  padding: 6px 10px;
  box-sizing: border-box;
}

.mock {
  display: flex;
  flex-direction: column;
  align-items: stretch;
  justify-content: center;
  gap: 4px;
  min-height: 52px;
}

/* 通用「输入框」底 */
.mock-field {
  height: 22px;
  border-radius: 4px;
  border: 1px solid var(--color-border-2);
  background: #fff;
  display: flex;
  align-items: center;
  padding: 0 8px;
}

.mock-input-line .mock-field::after {
  content: '';
  display: block;
  height: 6px;
  width: 42%;
  border-radius: 2px;
  background: var(--color-fill-3);
}

.mock-textarea {
  border-radius: 4px;
  border: 1px solid var(--color-border-2);
  background: #fff;
  padding: 6px 8px;
  gap: 5px;
  min-height: 54px;
}

.mock-line {
  height: 5px;
  border-radius: 2px;
  background: var(--color-fill-3);
  width: 100%;
}

.mock-line--short {
  width: 55%;
}

.mock-line--mid {
  width: 72%;
}

/* Markdown */
.mock-md {
  gap: 3px;
  min-height: 56px;
}

.mock-md-bar {
  display: flex;
  gap: 4px;
  align-items: center;
}

.mock-md-pill {
  width: 14px;
  height: 8px;
  border-radius: 2px;
  background: var(--color-fill-3);
}

.mock-md-pill--wide {
  width: 22px;
}

.mock-md-body {
  border-radius: 4px;
  border: 1px solid var(--color-border-2);
  background: #fff;
  padding: 6px 8px;
  display: flex;
  flex-direction: column;
  gap: 5px;
  flex: 1;
}

/* 下拉 */
.mock-select-inner {
  display: flex;
  align-items: center;
  justify-content: space-between;
  height: 24px;
  border-radius: 4px;
  border: 1px solid var(--color-border-2);
  background: #fff;
  padding: 0 6px 0 8px;
}

.mock-pill {
  width: 48%;
  height: 10px;
  border-radius: 3px;
  background: rgb(var(--primary-2));
  border: 1px solid rgba(var(--primary-6), 0.25);
}

.mock-select-pills {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 3px;
  flex: 1;
  min-width: 0;
}

.mock-select--multi .mock-select-pills {
  row-gap: 3px;
}

.mock-pill--sm {
  width: 30%;
  max-width: 36px;
  height: 8px;
}

.mock-pill--narrow {
  max-width: 28px;
}

.mock-chevron {
  width: 0;
  height: 0;
  border-left: 4px solid transparent;
  border-right: 4px solid transparent;
  border-top: 5px solid var(--color-text-3);
  flex-shrink: 0;
  opacity: 0.7;
}

/* 级联单选：树形下拉 */
.mock-tree-dd {
  gap: 3px;
  min-height: 58px;
}

.mock-tree-trigger {
  display: flex;
  align-items: center;
  justify-content: space-between;
  height: 22px;
  border-radius: 4px;
  border: 1px solid var(--color-border-2);
  background: #fff;
  padding: 0 6px 0 8px;
  flex-shrink: 0;
}

.mock-tree-trigger-ph {
  width: 45%;
  height: 6px;
  border-radius: 2px;
  background: var(--color-fill-3);
}

.mock-tree-chevron {
  width: 0;
  height: 0;
  border-left: 4px solid transparent;
  border-right: 4px solid transparent;
  border-top: 5px solid var(--color-text-3);
  opacity: 0.65;
  flex-shrink: 0;
}

.mock-tree-panel {
  border-radius: 4px;
  border: 1px solid var(--color-border-2);
  background: #fff;
  box-shadow: 0 2px 10px rgba(0, 0, 0, 0.06);
  padding: 5px 6px 6px;
  display: flex;
  flex-direction: column;
  gap: 0;
}

.mock-tree-line {
  display: flex;
  align-items: center;
  gap: 4px;
  min-height: 12px;
  margin-bottom: 3px;
}

.mock-tree-line:last-child {
  margin-bottom: 0;
}

.mock-tree-twist {
  width: 10px;
  height: 10px;
  flex-shrink: 0;
  display: flex;
  align-items: center;
  justify-content: center;
}

/* 展开节点：小三角向下 */
.mock-tree-twist--open {
  position: relative;
}

.mock-tree-twist--open::after {
  content: '';
  width: 0;
  height: 0;
  border-left: 3px solid transparent;
  border-right: 3px solid transparent;
  border-top: 4px solid var(--color-text-3);
  opacity: 0.75;
}

/* 叶子节点：圆点 */
.mock-tree-twist--dot::after {
  content: '';
  width: 4px;
  height: 4px;
  border-radius: 50%;
  background: var(--color-text-3);
  opacity: 0.45;
}

.mock-tree-bar {
  flex: 1;
  height: 8px;
  border-radius: 2px;
  background: var(--color-fill-3);
  min-width: 0;
}

.mock-tree-bar--mid {
  max-width: 78%;
  flex: none;
  width: 72%;
}

.mock-tree-bar--active {
  height: 10px;
  border-radius: 3px;
  background: rgb(var(--primary-1));
  border: 1px solid rgba(var(--primary-6), 0.35);
  max-width: 85%;
  flex: none;
  width: 80%;
}

.mock-tree-nest {
  margin-left: 6px;
  padding-left: 8px;
  border-left: 1px solid var(--color-border-2);
  display: flex;
  flex-direction: column;
  gap: 0;
}

.mock-tree-nest--deep {
  margin-top: 2px;
  margin-left: 6px;
  padding-left: 8px;
  border-left: 1px solid var(--color-border-2);
}

/* 关联 */
.mock-link {
  gap: 6px;
  min-height: 54px;
}

.mock-link-row {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 5px;
}

.mock-link-card {
  flex: 1;
  min-width: 0;
  max-width: 38%;
  height: 26px;
  border-radius: 4px;
  border: 1px solid var(--color-border-2);
  background: #fff;
  display: flex;
  align-items: center;
  padding: 0 6px;
}

.mock-link-card--to {
  max-width: 36%;
}

.mock-link-card-bar {
  height: 6px;
  width: 70%;
  border-radius: 2px;
  background: var(--color-fill-3);
}

.mock-link-card-bar--short {
  width: 55%;
}

.mock-link-join {
  width: 14px;
  height: 2px;
  border-radius: 1px;
  background: rgb(var(--primary-5));
  opacity: 0.55;
  flex-shrink: 0;
  position: relative;
}

.mock-link-join::after {
  content: '';
  position: absolute;
  right: -1px;
  top: 50%;
  transform: translateY(-50%);
  border: 3px solid transparent;
  border-left: 4px solid rgb(var(--primary-5));
  opacity: 0.65;
}

.mock-link-tags {
  display: flex;
  justify-content: center;
  align-items: center;
  gap: 4px;
  flex-wrap: wrap;
}

.mock-link-tag-pill {
  height: 10px;
  width: 52%;
  max-width: 120px;
  border-radius: 3px;
  background: rgb(var(--primary-1));
  border: 1px solid rgba(var(--primary-6), 0.3);
}

.mock-link-tag-pill--sm {
  width: 22%;
  max-width: 36px;
  height: 8px;
}

.mock-link--multi .mock-link-card--to .mock-link-card-bar--short {
  width: 65%;
}

/* 日期 */
.mock-date-input {
  display: flex;
  align-items: center;
  justify-content: space-between;
  height: 24px;
  border-radius: 4px;
  border: 1px solid var(--color-border-2);
  background: #fff;
  padding: 0 6px;
}

.mock-date-text {
  width: 58%;
  height: 6px;
  border-radius: 2px;
  background: var(--color-fill-3);
}

.mock-cal {
  width: 14px;
  height: 14px;
  border-radius: 3px;
  border: 1px solid var(--color-border-3);
  background: linear-gradient(
    to bottom,
    transparent 6px,
    var(--color-border-2) 6px,
    var(--color-border-2) 7px,
    transparent 7px
  );
  position: relative;
  flex-shrink: 0;
}

.mock-cal::before {
  content: '';
  position: absolute;
  top: 2px;
  left: 3px;
  right: 3px;
  height: 3px;
  border-radius: 1px;
  background: rgb(var(--primary-4));
  opacity: 0.35;
}

/* 数字 */
.mock-field--numeric {
  justify-content: flex-end;
  gap: 2px;
}

.mock-num-seg {
  width: 18%;
  height: 6px;
  border-radius: 2px;
  background: var(--color-text-2);
  opacity: 0.35;
}

.mock-num-seg--dot {
  width: 3px;
}

/* 附件 */
.mock-drop {
  display: flex;
  align-items: center;
  gap: 6px;
  min-height: 28px;
  border: 1px dashed var(--color-border-3);
  border-radius: 4px;
  background: rgba(255, 255, 255, 0.65);
  padding: 4px 8px;
}

.mock-clip {
  width: 12px;
  height: 14px;
  border-radius: 2px;
  border: 2px solid rgb(var(--primary-5));
  border-bottom-width: 3px;
  flex-shrink: 0;
  opacity: 0.65;
}

.mock-file-name {
  flex: 1;
  height: 6px;
  border-radius: 2px;
  background: var(--color-fill-3);
  max-width: 65%;
}

/* 链接 */
.mock-url-row {
  display: flex;
  align-items: center;
  gap: 6px;
}

.mock-link-ico {
  width: 12px;
  height: 12px;
  border-radius: 50%;
  border: 2px solid rgb(var(--primary-5));
  flex-shrink: 0;
  opacity: 0.5;
}

.mock-field--url {
  flex: 1;
}

.mock-field--url::after {
  content: '';
  display: block;
  height: 5px;
  width: 75%;
  border-radius: 2px;
  background: var(--color-fill-3);
}
</style>
