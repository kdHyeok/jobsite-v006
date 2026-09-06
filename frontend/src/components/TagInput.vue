<script setup lang="ts">
import { computed, nextTick, ref } from 'vue'

const props = withDefaults(defineProps<{
  modelValue: string[]
  suggestions: string[]
  placeholder?: string
  max?: number
}>(), { placeholder: '', max: 30 })

const emit = defineEmits<{ 'update:modelValue': [value: string[]] }>()
const input = ref('')
const inputElement = ref<HTMLInputElement | null>(null)
const open = ref(false)

const choices = computed(() => {
  const needle = input.value.trim().toLowerCase()
  return [...new Set(props.suggestions)]
    .filter((value) => !props.modelValue.includes(value))
    .filter((value) => !needle || value.toLowerCase().includes(needle))
    .slice(0, 10)
})

function add(raw = input.value) {
  const value = raw.trim()
  if (!value || props.modelValue.includes(value) || props.modelValue.length >= props.max) return
  emit('update:modelValue', [...props.modelValue, value])
  input.value = ''
}

function remove(value: string) {
  emit('update:modelValue', props.modelValue.filter((item) => item !== value))
}

async function edit(value: string) {
  remove(value)
  input.value = value
  open.value = true
  await nextTick()
  inputElement.value?.focus()
}

function closeLater() {
  window.setTimeout(() => { open.value = false }, 100)
}
</script>

<template>
  <div class="tag-input" :class="{ focused: open }">
    <span v-for="value in modelValue" :key="value" class="tag-chip">
      <button type="button" class="tag-chip__label" :aria-label="`${value} 이름 수정`" @click="edit(value)">{{ value }}</button>
      <button type="button" class="tag-chip__remove" :aria-label="`${value} 삭제`" @click="remove(value)">×</button>
    </span>
    <input
      ref="inputElement"
      v-model="input"
      :placeholder="modelValue.length ? '검색 또는 새 태그 입력' : placeholder"
      :aria-expanded="open"
      role="combobox"
      autocomplete="off"
      @focus="open = true"
      @blur="closeLater"
      @keydown.enter.prevent="add()"
    />
    <div v-if="open && choices.length" class="tag-input__menu" role="listbox">
      <button v-for="choice in choices" :key="choice" type="button" role="option" @mousedown.prevent="add(choice)">
        {{ choice }}
      </button>
    </div>
  </div>
</template>
