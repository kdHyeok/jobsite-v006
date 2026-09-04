<script setup lang="ts">
import { onMounted, onUnmounted, ref, watch } from 'vue'
import { ApiClientError, updateMyName } from '../api/auth'
import { avatarInitial, roleLabels } from '../types/auth'
import type { Me } from '../types/auth'

const props = defineProps<{ me: Me }>()
const emit = defineEmits<{ updated: [Me]; signedOut: [] }>()

const open = ref(false)
const name = ref(props.me.displayName ?? '')
const saving = ref(false)
const errorMessage = ref('')
const root = ref<HTMLElement | null>(null)

// 다른 곳에서 me 가 갱신되면 입력값도 따라간다.
watch(() => props.me.displayName, (next) => { name.value = next ?? '' })

function toggle() {
  open.value = !open.value
  errorMessage.value = ''
  if (open.value) name.value = props.me.displayName ?? ''
}

async function save() {
  saving.value = true
  errorMessage.value = ''
  try {
    emit('updated', await updateMyName(name.value))
    open.value = false
  } catch (error) {
    errorMessage.value = error instanceof ApiClientError ? error.message : '이름을 저장하지 못했습니다.'
  } finally {
    saving.value = false
  }
}

function onDocumentClick(event: MouseEvent) {
  if (open.value && root.value && !root.value.contains(event.target as Node)) open.value = false
}
function onKeydown(event: KeyboardEvent) {
  if (event.key === 'Escape') open.value = false
}

onMounted(() => {
  document.addEventListener('mousedown', onDocumentClick)
  document.addEventListener('keydown', onKeydown)
})
onUnmounted(() => {
  document.removeEventListener('mousedown', onDocumentClick)
  document.removeEventListener('keydown', onKeydown)
})
</script>

<template>
  <div ref="root" class="user-menu">
    <button
      type="button"
      class="avatar"
      :title="me.email ?? ''"
      aria-haspopup="dialog"
      :aria-expanded="open"
      @click="toggle"
    >
      {{ avatarInitial(me) }}
    </button>

    <section v-if="open" class="popover" role="dialog" aria-label="내 계정">
      <div class="popover__head">
        <strong>{{ me.displayName || '이름 없음' }}</strong>
        <span v-if="me.role" class="self-tag">{{ roleLabels[me.role] }}</span>
      </div>
      <p class="popover__email">{{ me.email }}</p>

      <form class="popover__form" @submit.prevent="save">
        <label class="field full">
          <span>표시 이름</span>
          <input v-model="name" maxlength="80" placeholder="비우면 이름을 지웁니다" :disabled="saving" />
          <span v-if="errorMessage" class="field-error">{{ errorMessage }}</span>
        </label>
        <div class="popover__actions">
          <button type="submit" class="button primary compact" :disabled="saving">
            {{ saving ? '저장 중…' : '저장' }}
          </button>
          <button type="button" class="button secondary compact" @click="emit('signedOut')">로그아웃</button>
        </div>
      </form>
    </section>
  </div>
</template>
