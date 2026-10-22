<template>
  <div class="rich-text" :class="`rt-${variant}`" v-html="html"></div>
</template>

<script setup>
import { computed } from 'vue'
import { renderRichText } from '../utils/richText'

const props = defineProps({
  /** 富文本内容（主 prop） */
  content: { type: String, default: '' },
  /** 兼容别名：等价于 content */
  text: { type: String, default: '' },
  /**
   * 展示场景，决定图片尺寸：
   * article 文章正文（可用满栏宽）/ comment 评论（略收窄）/ bubble 聊天气泡（小图）
   */
  variant: { type: String, default: 'article' }
})

const html = computed(() => renderRichText(props.content || props.text))
</script>

<style scoped>
.rich-text {
  font-size: inherit;
  line-height: 1.8;
  word-break: break-word;
  white-space: normal;
}

.rich-text :deep(a) {
  color: var(--c-shinigami);
  text-decoration: underline;
  text-underline-offset: 2px;
}

.rich-text :deep(a:hover) {
  color: var(--c-ninja);
}

/* 图片尺寸按场景分档。
   不再写死 max-height —— 否则长图会被压扁，配合 object-fit 还会出现黑边。 */
.rich-text :deep(img) {
  display: block;
  max-width: 100%;
  height: auto;
  margin: 10px 0;
  border-radius: 8px;
  border: 1px solid rgba(255, 255, 255, 0.12);
}

/* 评论：比正文略窄，避免一张图占满整个评论区 */
.rich-text.rt-comment :deep(img) {
  max-width: 420px;
}

/* 聊天气泡：保持小巧，不破坏左右气泡的对齐与排版 */
.rich-text.rt-bubble :deep(img) {
  max-width: 220px;
  margin: 4px 0;
}

/* 引用块（回复 / 引用消息的头） */
.rich-text :deep(blockquote) {
  margin: 2px 0 6px;
  padding: 4px 10px;
  border-left: 3px solid rgba(150, 182, 168, 0.36);
  background: rgba(255, 255, 255, 0.06);
  border-radius: 0 6px 6px 0;
  font-size: 0.88em;
  color: var(--c-dim);
}
.rich-text.rt-bubble :deep(blockquote) {
  margin: 0 0 4px;
}

.rich-text :deep(code) {
  padding: 1px 6px;
  font-size: 0.92em;
  font-family: Consolas, Monaco, monospace;
  border-radius: 4px;
  background: rgba(255, 255, 255, 0.1);
}

.rich-text :deep(.rt-file) {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  padding: 2px 10px;
  border-radius: 12px;
  font-size: 0.94em;
  color: var(--c-ninja);
  border: 1px solid rgba(143, 208, 174, 0.4);
  background: rgba(150, 182, 168, 0.09);
  text-decoration: none;
}
</style>
