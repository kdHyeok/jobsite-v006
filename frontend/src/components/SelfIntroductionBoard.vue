<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import {
  createSelfIntroduction,
  deleteSelfIntroduction,
  listSelfIntroductions,
  updateSelfIntroduction,
} from '../api/self-introductions'
import { listResumes } from '../api/resumes'
import type { ResumeSummary } from '../types/resume'
import type { SelfIntroduction, SelfIntroductionPayload } from '../types/self-introduction'
import { opensEditor } from '../utils/doubleClick'
import { highlightMatches } from '../utils/highlight'
import ConfirmDialog from './ConfirmDialog.vue'

const items = ref<SelfIntroduction[]>([])
const resumes = ref<ResumeSummary[]>([])
const query = ref('')
const activeQuery = ref('')
const loading = ref(true)
const saving = ref(false)
const errorMessage = ref('')
const editingId = ref<string | null>(null)
const deleteTarget = ref<SelfIntroduction | null>(null)
const draft = reactive<SelfIntroductionPayload>({ resumeId: '', question: '', answer: '' })

const resumeNames = computed(() => new Map(resumes.value.map((resume) => [resume.id, resume.name])))

async function load() {
  loading.value = true
  errorMessage.value = ''
  activeQuery.value = query.value.trim()
  try {
    items.value = await listSelfIntroductions(activeQuery.value)
  } catch (error) {
    errorMessage.value = error instanceof Error ? error.message : '자기소개 문항을 불러오지 못했습니다.'
  } finally {
    loading.value = false
  }
}

function openNew() {
  editingId.value = ''
  Object.assign(draft, { resumeId: resumes.value[0]?.id ?? '', question: '', answer: '' })
}

function openEdit(item: SelfIntroduction) {
  editingId.value = item.id
  Object.assign(draft, { resumeId: item.resumeId, question: item.question, answer: item.answer ?? '' })
}

function editFromDoubleClick(event: MouseEvent, item: SelfIntroduction) {
  if (opensEditor(event)) openEdit(item)
}

function closeEditor() {
  editingId.value = null
}

async function save() {
  if (!draft.resumeId || !draft.question.trim()) return
  saving.value = true
  errorMessage.value = ''
  const payload = { ...draft, question: draft.question.trim() }
  try {
    if (editingId.value) await updateSelfIntroduction(editingId.value, payload)
    else await createSelfIntroduction(payload)
    closeEditor()
    await load()
  } catch (error) {
    errorMessage.value = error instanceof Error ? error.message : '자기소개 문항을 저장하지 못했습니다.'
  } finally {
    saving.value = false
  }
}

async function remove() {
  if (!deleteTarget.value) return
  saving.value = true
  try {
    await deleteSelfIntroduction(deleteTarget.value.id)
    deleteTarget.value = null
    await load()
  } catch (error) {
    errorMessage.value = error instanceof Error ? error.message : '자기소개 문항을 삭제하지 못했습니다.'
  } finally {
    saving.value = false
  }
}

onMounted(async () => {
  try {
    resumes.value = await listResumes()
  } catch {
    resumes.value = []
  }
  await load()
})
</script>

<template>
  <main class="page">
    <div class="page-head">
      <div class="page-title">
        <h1>자기소개</h1>
        <span class="page-count">{{ items.length }}</span>
      </div>
      <button type="button" class="button primary" :disabled="resumes.length === 0" @click="openNew">문항 추가</button>
    </div>

    <form class="intro-search" role="search" @submit.prevent="load">
      <label class="field full">
        <span class="sr-only">질문과 답변 검색</span>
        <input v-model="query" maxlength="100" placeholder="질문과 답변에서 같은 단어 검색" />
      </label>
      <button type="submit" class="button secondary">검색</button>
      <button v-if="activeQuery" type="button" class="button ghost" @click="query = ''; load()">초기화</button>
    </form>

    <div v-if="errorMessage" class="notice error" role="alert">{{ errorMessage }}</div>
    <div v-if="resumes.length === 0" class="notice">먼저 이력서를 만든 뒤 자기소개 문항을 추가할 수 있습니다.</div>

    <section v-if="editingId !== null" class="resume-section intro-editor">
      <div class="resume-section__head"><h2>{{ editingId ? '문항 수정' : '문항 추가' }}</h2></div>
      <form class="form-grid" @submit.prevent="save">
        <label class="field full">
          <span>이력서</span>
          <select v-model="draft.resumeId" required>
            <option v-for="resume in resumes" :key="resume.id" :value="resume.id">{{ resume.name }}</option>
          </select>
        </label>
        <label class="field full">
          <span>질문</span>
          <textarea v-model="draft.question" maxlength="1000" rows="3" required />
        </label>
        <label class="field full">
          <span>답변</span>
          <textarea v-model="draft.answer" maxlength="10000" rows="10" placeholder="답변을 작성하세요." />
        </label>
        <div class="full page-tools">
          <button type="button" class="button secondary" @click="closeEditor">취소</button>
          <button type="submit" class="button primary" :disabled="saving || !draft.resumeId || !draft.question.trim()">
            {{ saving ? '저장 중…' : '저장' }}
          </button>
        </div>
      </form>
    </section>

    <section v-if="loading" class="loading-card" aria-live="polite"><span class="spinner" /> 검색 중입니다.</section>
    <section v-else-if="items.length === 0" class="empty-state">
      <strong>{{ activeQuery ? '검색 결과가 없습니다.' : '자기소개 문항이 없습니다.' }}</strong>
      <p>{{ activeQuery ? '다른 단어로 검색해 보세요.' : '이력서에서 질문을 추가하거나 여기서 새로 만드세요.' }}</p>
    </section>
    <section v-else class="intro-list" aria-label="자기소개 문항 목록">
      <article
        v-for="item in items"
        :key="item.id"
        class="intro-card"
        title="더블클릭해 자기소개 문항 수정"
        @dblclick="editFromDoubleClick($event, item)"
      >
        <div class="intro-card__head">
          <span class="chip">{{ resumeNames.get(item.resumeId) ?? '삭제된 이력서' }}</span>
          <div class="page-tools">
            <button type="button" class="button ghost compact" @click="openEdit(item)">수정</button>
            <button type="button" class="button danger compact" @click="deleteTarget = item">삭제</button>
          </div>
        </div>
        <h2 class="search-highlight">
          <template v-for="(segment, index) in highlightMatches(item.question, activeQuery)" :key="index">
            <mark v-if="segment.matched">{{ segment.text }}</mark><template v-else>{{ segment.text }}</template>
          </template>
        </h2>
        <p v-if="item.answer" class="body-copy search-highlight">
          <template v-for="(segment, index) in highlightMatches(item.answer, activeQuery)" :key="index">
            <mark v-if="segment.matched">{{ segment.text }}</mark><template v-else>{{ segment.text }}</template>
          </template>
        </p>
        <p v-else class="body-copy empty">아직 답변이 없습니다.</p>
      </article>
    </section>

    <ConfirmDialog
      v-if="deleteTarget"
      title="자기소개 문항을 삭제할까요?"
      :subject="deleteTarget.question"
      detail=" 문항과 답변이 영구 삭제됩니다."
      aria-label="자기소개 문항 삭제 확인"
      :busy="saving"
      @confirm="remove"
      @cancel="deleteTarget = null"
    />
  </main>
</template>
