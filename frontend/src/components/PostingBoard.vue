<script setup lang="ts">
import { onMounted, ref, watch } from 'vue'
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
import Drawer from './Drawer.vue'
import PostingCard from './PostingCard.vue'
import PostingDetail from './PostingDetail.vue'
import PostingForm from './PostingForm.vue'
import type { Company } from '../types/company'
import type { ApplicationStage, JobPosting, JobPostingPayload } from '../types/posting'

/** 드로어 하나가 보기·수정·추가를 모두 맡는다 — docs/design-system.md 규칙 4. */
type Mode = 'view' | 'edit' | 'create'

const tab = ref<'open' | 'archived'>('open')
const postings = ref<JobPosting[]>([])
const companies = ref<Company[]>([])
const loading = ref(true)
const loadError = ref('')
const notice = ref('')

const mode = ref<Mode | null>(null)
const selected = ref<JobPosting | null>(null)
const saving = ref(false)
const formErrors = ref<Record<string, string>>({})
const deleteTarget = ref<JobPosting | null>(null)
const deleting = ref(false)

/** 서버가 마감 임박 순으로 내려준다. 여기서 다시 정렬하면 두 곳이 어긋난다. */
async function load() {
  loading.value = true
  loadError.value = ''
  try {
    postings.value = tab.value === 'open' ? await listPostings() : await listArchivedPostings()
    // 열려 있는 드로어의 내용도 새 목록에 맞춘다.
    if (selected.value) {
      selected.value = postings.value.find((posting) => posting.id === selected.value?.id) ?? selected.value
    }
  } catch (error) {
    loadError.value = error instanceof Error ? error.message : '채용공고를 불러오지 못했습니다.'
  } finally {
    loading.value = false
  }
}

function open(posting: JobPosting) {
  selected.value = posting
  mode.value = 'view'
}

function openCreate() {
  selected.value = null
  formErrors.value = {}
  mode.value = 'create'
}

function startEdit() {
  formErrors.value = {}
  mode.value = 'edit'
}

function close() {
  mode.value = null
  selected.value = null
}

async function save(payload: JobPostingPayload) {
  saving.value = true
  formErrors.value = {}
  notice.value = ''
  const editingId = mode.value === 'edit' ? selected.value?.id ?? null : null
  try {
    const saved = editingId
      ? await updatePosting(editingId, payload)
      : await createPosting(payload)
    selected.value = saved
    mode.value = 'view'
    await load()
    notice.value = editingId ? '채용공고를 수정했습니다.' : '채용공고를 추가했습니다.'
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

async function changeStage(stage: ApplicationStage) {
  if (!selected.value) return
  notice.value = ''
  try {
    selected.value = await changePostingStage(selected.value.id, stage)
    await load()
  } catch (error) {
    loadError.value = error instanceof Error ? error.message : '지원단계를 바꾸지 못했습니다.'
  }
}

async function toggleArchive() {
  if (!selected.value) return
  const wasArchived = selected.value.archived
  notice.value = ''
  try {
    await setPostingArchived(selected.value.id, !wasArchived)
    close()
    await load()
    notice.value = wasArchived ? '공고를 다시 꺼냈습니다.' : '공고를 보관함으로 옮겼습니다.'
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
    close()
    await load()
    notice.value = '채용공고를 삭제했습니다.'
  } catch (error) {
    loadError.value = error instanceof Error ? error.message : '삭제 중 오류가 발생했습니다.'
  } finally {
    deleting.value = false
  }
}

watch(tab, () => {
  close()
  load()
})

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
  <main class="page">
    <div class="page-head">
      <div class="page-title">
        <h1>채용공고</h1>
        <span class="page-count">{{ postings.length }}</span>
      </div>
      <button type="button" class="button primary" @click="openCreate">공고 추가</button>
    </div>

    <div class="tabs" role="tablist" aria-label="공고 보기">
      <button type="button" role="tab" :aria-selected="tab === 'open'" :class="{ active: tab === 'open' }" @click="tab = 'open'">
        진행 중
      </button>
      <button type="button" role="tab" :aria-selected="tab === 'archived'" :class="{ active: tab === 'archived' }" @click="tab = 'archived'">
        보관함
      </button>
    </div>

    <div v-if="notice" class="notice success" role="status">{{ notice }}</div>
    <div v-if="loadError" class="notice error" role="alert">
      <span>{{ loadError }}</span>
      <button type="button" @click="load">다시 시도</button>
    </div>

    <section v-if="loading" class="loading-card" aria-live="polite">
      <span class="spinner" /> 불러오는 중입니다.
    </section>

    <section v-else-if="postings.length === 0" class="empty-state">
      <strong>{{ tab === 'open' ? '진행 중인 공고가 없습니다.' : '보관된 공고가 없습니다.' }}</strong>
      <p>마감이 지난 ‘관심’ 공고는 목록을 열 때 보관함으로 자동 이동합니다.</p>
      <button v-if="tab === 'open'" type="button" class="button primary" @click="openCreate">첫 공고 추가</button>
    </section>

    <section v-else class="card-grid">
      <PostingCard
        v-for="posting in postings"
        :key="posting.id"
        :posting="posting"
        :selected="selected?.id === posting.id"
        @select="open(posting)"
      />
    </section>
  </main>

  <Drawer
    v-if="mode === 'view' && selected"
    :title="selected.position"
    :subtitle="selected.companyName ?? '회사 미입력'"
    @close="close"
  >
    <PostingDetail :posting="selected" @change-stage="changeStage" />
    <template #actions>
      <button type="button" class="button danger" @click="deleteTarget = selected">삭제</button>
      <button type="button" class="button secondary" @click="toggleArchive">
        {{ selected.archived ? '되돌리기' : '보관' }}
      </button>
      <button type="button" class="button secondary" @click="startEdit">수정</button>
    </template>
  </Drawer>

  <Drawer
    v-else-if="mode === 'edit' || mode === 'create'"
    :title="mode === 'edit' ? '채용공고 수정' : '채용공고 추가'"
    :subtitle="mode === 'edit' ? selected?.position : undefined"
    @close="mode === 'edit' ? (mode = 'view') : close()"
  >
    <PostingForm
      :posting="mode === 'edit' ? selected : null"
      :companies="companies"
      :saving="saving"
      :api-field-errors="formErrors"
      @submit="save"
    />
    <template #actions>
      <button
        type="button"
        class="button secondary"
        :disabled="saving"
        @click="mode === 'edit' ? (mode = 'view') : close()"
      >
        취소
      </button>
      <button type="submit" form="posting-form" class="button primary" :disabled="saving">
        {{ saving ? '저장 중…' : '저장' }}
      </button>
    </template>
  </Drawer>

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
