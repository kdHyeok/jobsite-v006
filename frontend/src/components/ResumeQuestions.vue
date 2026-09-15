<script setup lang="ts">
import { onMounted, ref, watch } from 'vue'
import { createSelfIntroduction, listSelfIntroductions } from '../api/self-introductions'
import type { SelfIntroduction } from '../types/self-introduction'

const props = defineProps<{ resumeId: string }>()
const emit = defineEmits<{ openIntroductions: [] }>()

const items = ref<SelfIntroduction[]>([])
const question = ref('')
const saving = ref(false)
const errorMessage = ref('')

async function load() {
  try {
    items.value = await listSelfIntroductions('', props.resumeId)
  } catch (error) {
    errorMessage.value = error instanceof Error ? error.message : '문항을 불러오지 못했습니다.'
  }
}

async function add() {
  if (!question.value.trim()) return
  saving.value = true
  errorMessage.value = ''
  try {
    await createSelfIntroduction({ resumeId: props.resumeId, question: question.value.trim(), answer: '' })
    question.value = ''
    await load()
  } catch (error) {
    errorMessage.value = error instanceof Error ? error.message : '문항을 추가하지 못했습니다.'
  } finally {
    saving.value = false
  }
}

watch(() => props.resumeId, load)
onMounted(load)
</script>

<template>
  <section class="resume-section">
    <div class="resume-section__head">
      <h2>자기소개 문항 <span class="page-count">{{ items.length }}</span></h2>
      <button type="button" class="button secondary compact" @click="emit('openIntroductions')">답변 관리</button>
    </div>
    <form class="question-add" @submit.prevent="add">
      <label class="field full">
        <span>질문 추가</span>
        <input v-model="question" maxlength="1000" placeholder="예: 지원 동기와 입사 후 목표를 작성해 주세요." />
      </label>
      <button type="submit" class="button primary" :disabled="saving || !question.trim()">
        {{ saving ? '추가 중…' : '문항 추가' }}
      </button>
    </form>
    <p v-if="errorMessage" class="field-error" role="alert">{{ errorMessage }}</p>
    <p v-if="items.length === 0" class="body-copy empty">아직 자기소개 문항이 없습니다.</p>
    <ol v-else class="question-list">
      <li v-for="item in items" :key="item.id">{{ item.question }}</li>
    </ol>
  </section>
</template>
