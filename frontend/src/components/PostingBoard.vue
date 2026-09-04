<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue'
import { listCompanies } from '../api/companies'
import {
  ApiClientError,
  changePostingStage,
  createPosting,
  deletePosting,
  listArchivedPostings,
  listPostings,
  setPostingArchived,
  updatePosting,
} from '../api/postings'
import ConfirmDialog from './ConfirmDialog.vue'
import PostingCard from './PostingCard.vue'
import PostingForm from './PostingForm.vue'
import type { Company } from '../types/company'
import type { ApplicationStage, JobPosting, JobPostingPayload } from '../types/posting'
import { daysUntil, stageLabels } from '../types/posting'

const tab = ref<'open' | 'archived'>('open')
const postings = ref<JobPosting[]>([])
const companies = ref<Company[]>([])
const loading = ref(true)
const loadError = ref('')
const notice = ref('')
const formOpen = ref(false)
const editing = ref<JobPosting | null>(null)
const saving = ref(false)
const formErrors = ref<Record<string, string>>({})
const deleteTarget = ref<JobPosting | null>(null)
const deleting = ref(false)

/** 서버가 마감 임박 순으로 내려준다. 여기서 다시 정렬하면 두 곳이 어긋난다. */
const urgentCount = computed(() =>
  postings.value.filter((posting) => {
    const days = daysUntil(posting.deadlineAt)
    return days !== null && days >= 0 && days <= 7
  }).length,
)

async function load() {
  loading.value = true
  loadError.value = ''
  try {
    postings.value = tab.value === 'open' ? await listPostings() : await listArchivedPostings()
  } catch (error) {
    loadError.value = error instanceof Error ? error.message : '채용공고를 불러오지 못했습니다.'
  } finally {
    loading.value = false
  }
}

function openCreate() {
  editing.value = null
  formErrors.value = {}
  formOpen.value = true
}

function openEdit(posting: JobPosting) {
  editing.value = posting
  formErrors.value = {}
  formOpen.value = true
}

async function save(payload: JobPostingPayload) {
  saving.value = true
  formErrors.value = {}
  notice.value = ''
  try {
    const wasEditing = editing.value !== null
    if (editing.value) await updatePosting(editing.value.id, payload)
    else await createPosting(payload)
    formOpen.value = false
    await load()
    notice.value = wasEditing ? '채용공고를 수정했습니다.' : '채용공고를 추가했습니다.'
  } catch (error) {
    if (error instanceof ApiClientError) {
      formErrors.value = error.fieldErrors
      if (Object.keys(error.fieldErrors).length === 0) loadError.value = error.message
    } else {
      loadError.value = '저장 중 오류가 발생했습니다.'
    }
  } finally {
    saving.value = false
  }
}

async function changeStage(posting: JobPosting, stage: ApplicationStage) {
  notice.value = ''
  try {
    await changePostingStage(posting.id, stage)
    await load()
  } catch (error) {
    loadError.value = error instanceof Error ? error.message : '지원단계를 바꾸지 못했습니다.'
  }
}

async function toggleArchive(posting: JobPosting) {
  notice.value = ''
  try {
    await setPostingArchived(posting.id, !posting.archived)
    await load()
    notice.value = posting.archived ? '공고를 다시 꺼냈습니다.' : '공고를 보관함으로 옮겼습니다.'
  } catch (error) {
    loadError.value = error instanceof Error ? error.message : '보관 상태를 바꾸지 못했습니다.'
  }
}

async function remove() {
  if (!deleteTarget.value) return
  deleting.value = true
  notice.value = ''
  try {
    await deletePosting(deleteTarget.value.id)
    deleteTarget.value = null
    await load()
    notice.value = '채용공고를 삭제했습니다.'
  } catch (error) {
    loadError.value = error instanceof Error ? error.message : '삭제 중 오류가 발생했습니다.'
  } finally {
    deleting.value = false
  }
}

watch(tab, load)

onMounted(async () => {
  await load()
  // 폼의 기업 선택지. 실패해도 직접 입력이 가능하니 화면을 막지 않는다.
  try {
    companies.value = await listCompanies()
  } catch {
    companies.value = []
  }
})
</script>

<template>
  <main class="workspace">
    <section class="hero">
      <div>
        <p class="eyebrow">JOB POSTINGS · D-DAY</p>
        <h1>마감이 가까운 공고부터 처리하세요.</h1>
        <p>마감이 지난 ‘관심’ 공고는 목록을 열 때 보관함으로 자동 이동합니다.</p>
      </div>
      <div class="hero-aside">
        <div class="metric-card">
          <strong>{{ urgentCount }}</strong>
          <span>7일 내 마감</span>
        </div>
        <button type="button" class="button primary" @click="openCreate">+ 공고 추가</button>
      </div>
    </section>

    <div class="tab-bar" role="tablist" aria-label="공고 보기">
      <button
        type="button"
        role="tab"
        :aria-selected="tab === 'open'"
        :class="{ active: tab === 'open' }"
        @click="tab = 'open'"
      >
        진행 중
      </button>
      <button
        type="button"
        role="tab"
        :aria-selected="tab === 'archived'"
        :class="{ active: tab === 'archived' }"
        @click="tab = 'archived'"
      >
        보관함
      </button>
    </div>

    <div v-if="notice" class="notice success" role="status">{{ notice }}</div>
    <div v-if="loadError" class="notice error" role="alert">
      <span>{{ loadError }}</span>
      <button type="button" @click="load">다시 시도</button>
    </div>

    <section v-if="loading" class="loading-card" aria-live="polite">
      <span class="spinner" /> 채용공고를 불러오는 중입니다.
    </section>

    <section v-else-if="postings.length === 0" class="empty-state detail-empty">
      <span class="empty-icon">🗂</span>
      <strong>{{ tab === 'open' ? '진행 중인 공고가 없습니다.' : '보관된 공고가 없습니다.' }}</strong>
      <p>공고를 추가하면 마감 임박 순으로 정렬됩니다.</p>
      <button v-if="tab === 'open'" type="button" class="button primary" @click="openCreate">첫 공고 추가</button>
    </section>

    <section v-else class="posting-grid">
      <PostingCard v-for="posting in postings" :key="posting.id" :posting="posting">
        <template #actions>
          <label class="stage-select">
            <span class="sr-only">지원단계</span>
            <select
              :value="posting.stage"
              @change="changeStage(posting, ($event.target as HTMLSelectElement).value as ApplicationStage)"
            >
              <option v-for="(label, value) in stageLabels" :key="value" :value="value">{{ label }}</option>
            </select>
          </label>
          <button type="button" class="button secondary compact" @click="openEdit(posting)">수정</button>
          <button type="button" class="button secondary compact" @click="toggleArchive(posting)">
            {{ posting.archived ? '되돌리기' : '보관' }}
          </button>
          <button type="button" class="button danger compact" @click="deleteTarget = posting">삭제</button>
        </template>
      </PostingCard>
    </section>
  </main>

  <PostingForm
    v-if="formOpen"
    :posting="editing"
    :companies="companies"
    :saving="saving"
    :api-field-errors="formErrors"
    @submit="save"
    @cancel="formOpen = false"
  />
  <ConfirmDialog
    v-if="deleteTarget"
    title="채용공고를 삭제할까요?"
    :subject="deleteTarget.position"
    detail=" 공고 기록이 영구 삭제됩니다."
    aria-label="채용공고 삭제 확인"
    :busy="deleting"
    @confirm="remove"
    @cancel="deleteTarget = null"
  />
</template>
