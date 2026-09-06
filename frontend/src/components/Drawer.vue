<script setup lang="ts">
import { onMounted, onUnmounted } from 'vue'

defineProps<{ title: string; subtitle?: string }>()
const emit = defineEmits<{ close: [] }>()

function onKeydown(event: KeyboardEvent) {
  if (event.key === 'Escape') emit('close')
}

onMounted(() => document.addEventListener('keydown', onKeydown))
onUnmounted(() => document.removeEventListener('keydown', onKeydown))
</script>

<template>
  <div class="drawer-backdrop" @mousedown.self="emit('close')" />
  <aside class="drawer" role="dialog" aria-modal="true" :aria-label="title">
    <header class="drawer__head">
      <div class="drawer__title">
        <div>
          <h2>{{ title }}</h2>
          <p v-if="subtitle" class="drawer__subtitle">{{ subtitle }}</p>
        </div>
        <button type="button" class="icon-button" aria-label="닫기" @click="emit('close')">×</button>
      </div>
      <!-- 동작 버튼은 헤더 바로 아래 한 줄 — docs/design-system.md 규칙 4 -->
      <div v-if="$slots.actions" class="drawer__actions">
        <slot name="actions" />
      </div>
    </header>

    <div class="drawer__body">
      <slot />
    </div>
  </aside>
</template>
