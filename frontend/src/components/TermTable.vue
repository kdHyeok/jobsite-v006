<script setup lang="ts">
import type { ColumnConfig, TermRow } from '../types/resume'

/**
 * 행 안의 표(학년별 이수 내역 등). 바깥 행과 같은 규칙을 쓴다 —
 * :key 는 index, 값은 :value + @input, 불변으로 갈아끼운다.
 */
const props = defineProps<{ label: string; columns: ColumnConfig[]; rows: TermRow[] }>()
const emit = defineEmits<{ 'update:rows': [TermRow[]] }>()

const blank = () => Object.fromEntries(props.columns.map((c) => [c.key, ''])) as TermRow

function add() { emit('update:rows', [...props.rows, blank()]) }
function remove(index: number) { emit('update:rows', props.rows.filter((_, i) => i !== index)) }
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
  <div class="field full term-table">
    <span class="term-table__head">
      {{ label }} <span class="page-count">{{ rows.length }}</span>
      <button type="button" class="button ghost compact" @click="add">+ 행 추가</button>
    </span>

    <p v-if="rows.length === 0" class="body-copy empty">아직 입력한 내역이 없습니다.</p>

    <div v-else class="term-table__scroll">
      <table>
        <thead>
          <tr>
            <th class="term-table__seq">#</th>
            <th v-for="column in columns" :key="column.key" :class="{ 'term-table__wide': column.wide }">
              {{ column.label }}
            </th>
            <th class="term-table__tools">조작</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="(row, index) in rows" :key="index">
            <td class="term-table__seq">{{ index + 1 }}</td>
            <td v-for="column in columns" :key="column.key" :class="{ 'term-table__wide': column.wide }">
              <textarea
                v-if="column.wide"
                :value="row[column.key] ?? ''"
                rows="4"
                :aria-label="column.label"
                :placeholder="column.placeholder"
                @input="set(index, column.key, ($event.target as HTMLTextAreaElement).value)"
              />
              <input
                v-else
                :value="row[column.key] ?? ''"
                maxlength="200"
                :aria-label="column.label"
                :placeholder="column.placeholder"
                @input="set(index, column.key, ($event.target as HTMLInputElement).value)"
              />
            </td>
            <td class="term-table__tools">
              <span class="repeater__move">
                <button type="button" class="icon-button" aria-label="위로" :disabled="index === 0" @click="move(index, -1)">↑</button>
                <button type="button" class="icon-button" aria-label="아래로" :disabled="index === rows.length - 1" @click="move(index, 1)">↓</button>
                <button type="button" class="icon-button" aria-label="행 삭제" @click="remove(index)">×</button>
              </span>
            </td>
          </tr>
        </tbody>
      </table>
    </div>
  </div>
</template>
