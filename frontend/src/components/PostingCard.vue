<script setup lang="ts">
import type { JobPosting } from '../types/posting'
import { ddayLabel, ddayTone, statusLabels, statusTone } from '../types/posting'

defineProps<{ posting: JobPosting; selected?: boolean; moving?: boolean }>()
const emit = defineEmits<{ select: []; dragStart: [event: DragEvent]; dragEnd: [] }>()
</script>

<template>
  <button
    type="button"
    class="card"
    :class="{ selected, moving }"
    draggable="true"
    :aria-grabbed="moving"
    @click="emit('select')"
    @dragstart="emit('dragStart', $event)"
    @dragend="emit('dragEnd')"
  >
    <span class="card__top">
      <span class="card__title">{{ posting.title }}</span>
      <span class="dday" :data-tone="ddayTone(posting.deadlineAt)">{{ ddayLabel(posting.deadlineAt) }}</span>
    </span>
    <span class="card__sub">{{ posting.companyName || '회사 미입력' }}</span>
    <span class="card__chips">
      <span class="chip" :data-tone="statusTone[posting.status]">{{ statusLabels[posting.status] }}</span>
      <span v-if="posting.positions.length > 1" class="chip">직무 {{ posting.positions.length }}</span>
    </span>
  </button>
</template>
