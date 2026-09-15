<script setup lang="ts">
import { computed } from 'vue'
import { inlineTokens, parseMarkdown } from '../utils/markdown'

const props = withDefaults(defineProps<{ source: string | null; empty?: string }>(), {
  empty: '미입력',
})

const blocks = computed(() => parseMarkdown(props.source ?? ''))
</script>

<template>
  <p v-if="blocks.length === 0" class="body-copy empty">{{ empty }}</p>
  <div v-else class="markdown-memo">
    <template v-for="(block, blockIndex) in blocks" :key="blockIndex">
      <component :is="`h${block.level}`" v-if="block.kind === 'heading'">
        <template v-for="(token, tokenIndex) in inlineTokens(block.text)" :key="tokenIndex">
          <a v-if="token.kind === 'link'" :href="token.href" target="_blank" rel="noopener noreferrer">{{ token.text }}</a>
          <template v-else>{{ token.text }}</template>
        </template>
      </component>
      <p v-else-if="block.kind === 'paragraph'">
        <template v-for="(token, tokenIndex) in inlineTokens(block.text)" :key="tokenIndex">
          <a v-if="token.kind === 'link'" :href="token.href" target="_blank" rel="noopener noreferrer">{{ token.text }}</a>
          <template v-else>{{ token.text }}</template>
        </template>
      </p>
      <component :is="block.kind" v-else>
        <li
          v-for="(item, itemIndex) in block.items"
          :key="itemIndex"
          :value="item.value"
          :style="{ marginInlineStart: `${item.indent * 1.5}rem` }"
        >
          <template v-for="(token, tokenIndex) in inlineTokens(item.text)" :key="tokenIndex">
            <a v-if="token.kind === 'link'" :href="token.href" target="_blank" rel="noopener noreferrer">{{ token.text }}</a>
            <template v-else>{{ token.text }}</template>
          </template>
        </li>
      </component>
    </template>
  </div>
</template>

<style scoped>
.markdown-memo { line-height: 1.6; overflow-wrap: anywhere; }
.markdown-memo :is(h1, h2, h3, h4, h5, h6) { margin: .8rem 0 .35rem; line-height: 1.3; }
.markdown-memo h1 { font-size: 1.55rem; }
.markdown-memo h2 { font-size: 1.4rem; }
.markdown-memo h3 { font-size: 1.25rem; }
.markdown-memo h4 { font-size: 1.1rem; }
.markdown-memo h5 { font-size: 1rem; }
.markdown-memo h6 { font-size: .9rem; }
.markdown-memo p { margin: .35rem 0; white-space: pre-wrap; }
.markdown-memo :is(ul, ol) { margin: .35rem 0; padding-inline-start: 1.5rem; }
.markdown-memo a { color: var(--primary); }
</style>
