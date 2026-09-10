<script setup lang="ts">
import type { BusinessArea } from '../types/company'

/** 기업 주요 사업 반복 입력. 이름이 빈 행은 서버가 버린다. */
const props = defineProps<{ areas: BusinessArea[] }>()
const emit = defineEmits<{ 'update:areas': [BusinessArea[]] }>()

const MAX = 20

function add() {
  if (props.areas.length >= MAX) return
  emit('update:areas', [...props.areas, { name: '', description: '' }])
}
function remove(index: number) { emit('update:areas', props.areas.filter((_, i) => i !== index)) }
function move(index: number, delta: number) {
  const target = index + delta
  if (target < 0 || target >= props.areas.length) return
  const next = [...props.areas]
  const [area] = next.splice(index, 1)
  next.splice(target, 0, area)
  emit('update:areas', next)
}
function set(index: number, key: 'name' | 'description', value: string) {
  emit('update:areas', props.areas.map((area, i) => (i === index ? { ...area, [key]: value } : area)))
}
</script>

<template>
  <div class="field full">
    <span class="field-label-with-tools">
      주요 사업 <small>{{ areas.length }} / {{ MAX }}</small>
      <button type="button" class="button ghost compact" :disabled="areas.length >= MAX" @click="add">+ 사업 추가</button>
    </span>

    <p v-if="areas.length === 0" class="body-copy empty">아직 등록한 주요 사업이 없습니다.</p>

    <div v-for="(area, index) in areas" :key="index" class="repeater__row repeater__row--business">
      <span class="repeater__seq">{{ index + 1 }}</span>
      <input
        :value="area.name"
        maxlength="120"
        placeholder="사업명 (예: 반도체 소재)"
        aria-label="사업명"
        @input="set(index, 'name', ($event.target as HTMLInputElement).value)"
      />
      <span class="repeater__move">
        <button type="button" class="icon-button" aria-label="위로" :disabled="index === 0" @click="move(index, -1)">↑</button>
        <button type="button" class="icon-button" aria-label="아래로" :disabled="index === areas.length - 1" @click="move(index, 1)">↓</button>
        <button type="button" class="icon-button" aria-label="사업 삭제" @click="remove(index)">×</button>
      </span>
      <textarea
        class="repeater__note"
        :value="area.description ?? ''"
        maxlength="2000"
        rows="2"
        placeholder="주요 제품·매출 비중·전략 등"
        aria-label="사업 설명"
        @input="set(index, 'description', ($event.target as HTMLTextAreaElement).value)"
      />
    </div>
  </div>
</template>
