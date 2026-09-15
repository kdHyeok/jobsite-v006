<script setup lang="ts">
import { nextTick, ref } from 'vue'
import { indentSelection } from '../utils/markdown'

defineOptions({ inheritAttrs: false })

const model = defineModel<string>({ required: true })
const textarea = ref<HTMLTextAreaElement>()

async function onKeydown(event: KeyboardEvent) {
  if (event.key !== 'Tab' || !textarea.value) return
  event.preventDefault()
  const edit = indentSelection(
    model.value,
    textarea.value.selectionStart,
    textarea.value.selectionEnd,
    event.shiftKey,
  )
  model.value = edit.value
  await nextTick()
  textarea.value?.setSelectionRange(edit.start, edit.end)
}
</script>

<template>
  <textarea ref="textarea" v-model="model" v-bind="$attrs" @keydown="onKeydown" />
</template>
