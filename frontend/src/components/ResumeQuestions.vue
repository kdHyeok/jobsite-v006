<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue'
import { createSelfIntroduction, listSelfIntroductions, updateSelfIntroduction } from '../api/self-introductions'
import type { SelfIntroduction } from '../types/self-introduction'

const props = defineProps<{ resumeId: string }>()
const emit = defineEmits<{ openIntroductions: [] }>()
const items = ref<SelfIntroduction[]>([])
const matches = ref<SelfIntroduction[]>([])
const question = ref('')
const answer = ref('')
const query = ref('')
const saving = ref(false)
const errorMessage = ref('')
const linkedIds = computed(() => new Set(items.value.map((item) => item.id)))

async function load() {
  try { items.value = await listSelfIntroductions('', props.resumeId) }
  catch (error) { errorMessage.value = error instanceof Error ? error.message : '문항을 불러오지 못했습니다.' }
}

async function add() {
  if (!question.value.trim()) return
  saving.value = true
  errorMessage.value = ''
  try {
    await createSelfIntroduction({ resumeIds: [props.resumeId], question: question.value.trim(), answer: answer.value.trim() })
    question.value = ''
    answer.value = ''
    await load()
  } catch (error) {
    errorMessage.value = error instanceof Error ? error.message : '문항을 추가하지 못했습니다.'
  } finally { saving.value = false }
}

async function search() {
  matches.value = query.value.trim() ? await listSelfIntroductions(query.value.trim()) : []
}

async function attach(item: SelfIntroduction) {
  saving.value = true
  try {
    await updateSelfIntroduction(item.id, { question: item.question, answer: item.answer ?? '', resumeIds: [...new Set([...item.resumeIds, props.resumeId])] })
    await load()
  } finally { saving.value = false }
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
    <form class="question-add form-grid" @submit.prevent="add">
      <label class="field full"><span>질문 추가</span><input v-model="question" maxlength="1000" placeholder="예: 지원 동기와 입사 후 목표를 작성해 주세요." /></label>
      <label class="field full"><span>답변 (선택)</span><textarea v-model="answer" maxlength="10000" rows="4" placeholder="답변은 나중에 작성해도 됩니다." /></label>
      <button type="submit" class="button primary" :disabled="saving || !question.trim()">{{ saving ? '추가 중…' : '문항 추가' }}</button>
    </form>
    <form class="question-search" @submit.prevent="search">
      <label class="field full"><span>과거 질문·답변 검색</span><input v-model="query" maxlength="200" placeholder="같은 단어로 검색" /></label>
      <button type="submit" class="button secondary" :disabled="!query.trim()">검색</button>
    </form>
    <ul v-if="matches.length" class="question-results">
      <li v-for="item in matches" :key="item.id">
        <span><strong>{{ item.question }}</strong><small v-if="item.answer">{{ item.answer }}</small></span>
        <button type="button" class="button ghost compact" :disabled="saving || linkedIds.has(item.id)" @click="attach(item)">{{ linkedIds.has(item.id) ? '추가됨' : '추가' }}</button>
      </li>
    </ul>
    <p v-if="errorMessage" class="field-error" role="alert">{{ errorMessage }}</p>
    <p v-if="items.length === 0" class="body-copy empty">아직 자기소개 문항이 없습니다.</p>
    <ol v-else class="question-list">
      <li v-for="item in items" :key="item.id"><strong>{{ item.question }}</strong><p v-if="item.answer">{{ item.answer }}</p></li>
    </ol>
  </section>
</template>
