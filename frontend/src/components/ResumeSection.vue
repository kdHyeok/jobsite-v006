<script setup lang="ts">
import type { Row, SectionConfig } from '../types/resume'
import { MAX_LENGTH, emptyRow } from '../types/resume'

/**
 * 섹션 하나 = 행 카드들 + 행 추가. 행은 불변으로 갈아끼운다(부모의 변경 감지가 깊은 비교라서).
 * 행의 :key 는 index 다. 값은 :value + @input 이라 ↑↓ 뒤에도 입력값이 따라간다 — v-model 로 바꾸지 말 것.
 */
const props = defineProps<{ section: SectionConfig; rows: Row[] }>()
const emit = defineEmits<{ 'update:rows': [Row[]] }>()

function add() {
  emit('update:rows', [...props.rows, emptyRow(props.section)])
}
function remove(index: number) {
  emit('update:rows', props.rows.filter((_, i) => i !== index))
}
function move(index: number, delta: number) {
  const target = index + delta
  if (target < 0 || target >= props.rows.length) return
  const next = [...props.rows]
  const [row] = next.splice(index, 1)
  next.splice(target, 0, row)
  emit('update:rows', next)
}
function set(index: number, key: string, value: string) {
  emit('update:rows', props.rows.map((row, i) => (i === index ? { ...row, [key]: value } : row)))
}
</script>

<template>
  <section class="resume-section">
    <div class="resume-section__head">
      <h2>{{ section.label }} <span class="page-count">{{ rows.length }}</span></h2>
      <button type="button" class="button ghost compact" @click="add">+ 행 추가</button>
    </div>

    <p v-if="rows.length === 0" class="body-copy empty">아직 행이 없습니다.</p>

    <article v-for="(row, index) in rows" :key="index" class="resume-row">
      <div class="resume-row__bar">
        <span class="repeater__seq">{{ index + 1 }}</span>
        <span class="repeater__move">
          <button type="button" class="icon-button" aria-label="위로" :disabled="index === 0" @click="move(index, -1)">↑</button>
          <button type="button" class="icon-button" aria-label="아래로" :disabled="index === rows.length - 1" @click="move(index, 1)">↓</button>
          <button type="button" class="icon-button" aria-label="행 삭제" @click="remove(index)">×</button>
        </span>
      </div>
      <div class="form-grid">
        <label
          v-for="field in section.fields"
          :key="field.key"
          class="field"
          :class="{ full: field.kind === 'textarea' || field.kind === 'url' }"
        >
          <span>{{ field.label }}</span>
          <textarea
            v-if="field.kind === 'textarea'"
            :value="row[field.key] ?? ''"
            :maxlength="MAX_LENGTH.textarea"
            rows="3"
            @input="set(index, field.key, ($event.target as HTMLTextAreaElement).value)"
          />
          <input
            v-else
            :value="row[field.key] ?? ''"
            :maxlength="MAX_LENGTH[field.kind]"
            :placeholder="field.placeholder"
            @input="set(index, field.key, ($event.target as HTMLInputElement).value)"
          />
        </label>
      </div>
    </article>
  </section>
</template>
