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
      <div>
        <h2>{{ title }}</h2>
        <p v-if="subtitle" class="drawer__subtitle">{{ subtitle }}</p>
      </div>
      <button type="button" class="icon-button" aria-label="닫기" @click="emit('close')">×</button>
    </header>

    <div class="drawer__body">
      <slot />
    </div>

    <footer v-if="$slots.actions" class="drawer__foot">
      <slot name="actions" />
    </footer>
  </aside>
</template>
