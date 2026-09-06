<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue'
import {
  ApiClientError,
  deletePosition,
  listPositions,
  replacePositionReferences,
  updatePosition,
} from '../api/positions'
import { createReference, listReferences } from '../api/references'
import ConfirmDialog from './ConfirmDialog.vue'
import Drawer from './Drawer.vue'
import PositionDetail from './PositionDetail.vue'
import PositionForm from './PositionForm.vue'
import type { Position, PositionPayload, ReferenceItem, ReferencePayload } from '../types/position'
import { ddayLabel, ddayTone } from '../types/posting'

const props = defineProps<{ focus: string | null }>()

type Mode = 'view' | 'edit'

const positions = ref<Position[]>([])
const references = ref<ReferenceItem[]>([])
const query = ref('')
const loading = ref(true)
const loadError = ref('')
const notice = ref('')

const mode = ref<Mode | null>(null)
const selected = ref<Position | null>(null)
const saving = ref(false)
const formErrors = ref<Record<string, string>>({})
const deleteTarget = ref<Position | null>(null)
const deleting = ref(false)

/** 검색은 클라이언트 필터. 이름·회사·공고·팀·스택. 수백 건을 넘으면 서버 ?q= 로. */
const visible = computed(() => {
  const needle = query.value.trim().toLowerCase()
  if (!needle) return positions.value
  return positions.value.filter((p) =>
    [p.name, p.companyName, p.postingTitle, p.team, ...p.techStack]
      .some((v) => (v ?? '').toLowerCase().includes(needle)))
})

async function load() {
  loading.value = true
  loadError.value = ''
  try {
    positions.value = await listPositions()
    if (selected.value) {
      selected.value = positions.value.find((p) => p.id === selected.value?.id) ?? selected.value
    }
  } catch (error) {
    loadError.value = error instanceof Error ? error.message : '모집 직무를 불러오지 못했습니다.'
  } finally {
    loading.value = false
  }
}

async function loadReferences() {
  try {
    references.value = await listReferences()
  } catch {
    references.value = []
  }
}

function open(position: Position) {
  selected.value = position
  mode.value = 'view'
}
function close() {
  mode.value = null
  selected.value = null
}

/** 다른 화면이 넘긴 항목, 또는 참고 직무 카드가 가리키는 직무를 연다. */
function openById(id: string | null) {
  const found = positions.value.find((position) => position.id === id)
  if (found) open(found)
}

function fail(error: unknown, fallback: string) {
  if (error instanceof ApiClientError) {
    formErrors.value = error.fieldErrors
    if (Object.keys(error.fieldErrors).length === 0) loadError.value = error.message
  } else {
    loadError.value = fallback
  }
}

async function save(payload: PositionPayload) {
  if (!selected.value) return
  saving.value = true
  formErrors.value = {}
  notice.value = ''
  try {
    selected.value = await updatePosition(selected.value.id, payload)
    mode.value = 'view'
    await load()
    notice.value = '직무 정보를 수정했습니다.'
  } catch (error) {
    fail(error, '저장 중 오류가 발생했습니다.')
  } finally {
    saving.value = false
  }
}

async function setReferences(ids: string[]) {
  if (!selected.value) return
  saving.value = true
  try {
    selected.value = await replacePositionReferences(selected.value.id, ids)
    await load()
  } catch (error) {
    fail(error, '참고 정보를 바꾸지 못했습니다.')
  } finally {
    saving.value = false
  }
}

const attach = (id: string) =>
  setReferences([...(selected.value?.references.map((r) => r.id) ?? []), id])
const detach = (id: string) =>
  setReferences((selected.value?.references ?? []).map((r) => r.id).filter((x) => x !== id))

async function createAndAttach(payload: ReferencePayload) {
  saving.value = true
  try {
    const created = await createReference(payload)
    references.value = [created, ...references.value]
    await attach(created.id)
  } catch (error) {
    fail(error, '참고 정보를 만들지 못했습니다.')
  } finally {
    saving.value = false
  }
}

async function remove() {
  if (!deleteTarget.value) return
  deleting.value = true
  notice.value = ''
  try {
    await deletePosition(deleteTarget.value.id)
    deleteTarget.value = null
    close()
    await load()
    notice.value = '직무를 삭제했습니다.'
  } catch (error) {
    deleteTarget.value = null
    fail(error, '삭제 중 오류가 발생했습니다.')
  } finally {
    deleting.value = false
  }
}

watch(() => props.focus, () => openById(props.focus))

onMounted(async () => {
  await load()
  openById(props.focus)
  await loadReferences()
})
</script>

<template>
  <main class="page">
    <div class="page-head">
      <div class="page-title">
        <h1>모집 직무</h1>
        <span class="page-count">{{ visible.length }}</span>
      </div>
      <input v-model="query" class="search" type="search" placeholder="직무·회사·공고·팀·스택 검색" aria-label="직무 검색" />
    </div>

    <div v-if="notice" class="notice success" role="status">{{ notice }}</div>
    <div v-if="loadError" class="notice error" role="alert">
      <span>{{ loadError }}</span>
      <button type="button" @click="load">다시 시도</button>
    </div>

    <section v-if="loading" class="loading-card" aria-live="polite">
      <span class="spinner" /> 불러오는 중입니다.
    </section>

    <section v-else-if="visible.length === 0" class="empty-state">
      <strong>{{ query ? '검색 결과가 없습니다.' : '모집 직무가 없습니다.' }}</strong>
      <p>직무는 채용공고를 추가하면 함께 생깁니다.</p>
    </section>

    <section v-else class="card-grid">
      <button
        v-for="position in visible"
        :key="position.id"
        type="button"
        class="card"
        :class="{ selected: selected?.id === position.id }"
        @click="open(position)"
      >
        <span class="card__top">
          <span class="card__title">{{ position.name }}</span>
          <span class="dday" :data-tone="ddayTone(position.deadlineAt)">{{ ddayLabel(position.deadlineAt) }}</span>
        </span>
        <span class="card__sub">{{ position.companyName ?? '회사 미입력' }}<template v-if="position.postingTitle && position.postingTitle !== position.name"> · {{ position.postingTitle }}</template></span>
        <span v-if="position.techStack.length" class="card__chips">
          <span v-for="tech in position.techStack.slice(0, 3)" :key="tech" class="chip">{{ tech }}</span>
          <span v-if="position.techStack.length > 3" class="chip">+{{ position.techStack.length - 3 }}</span>
        </span>
      </button>
    </section>
  </main>

  <Drawer
    v-if="mode === 'view' && selected"
    :title="selected.name"
    :subtitle="[selected.companyName, selected.postingTitle].filter(Boolean).join(' · ')"
    @close="close"
  >
    <template #actions>
      <button type="button" class="button secondary compact" @click="formErrors = {}; mode = 'edit'">수정</button>
      <button type="button" class="button danger compact" @click="deleteTarget = selected">삭제</button>
    </template>
    <PositionDetail
      :position="selected"
      :all-references="references"
      :all-positions="positions"
      :busy="saving"
      @attach="attach"
      @detach="detach"
      @create="createAndAttach"
      @open-position="openById"
    />
  </Drawer>

  <Drawer
    v-else-if="mode === 'edit' && selected"
    title="직무 정보 수정"
    :subtitle="selected.name"
    @close="mode = 'view'"
  >
    <template #actions>
      <button type="button" class="button secondary compact" :disabled="saving" @click="mode = 'view'">취소</button>
      <button type="submit" form="position-form" class="button primary compact" :disabled="saving">
        {{ saving ? '저장 중…' : '저장' }}
      </button>
    </template>
    <PositionForm :position="selected" :saving="saving" :api-field-errors="formErrors" @submit="save" />
  </Drawer>

  <ConfirmDialog
    v-if="deleteTarget"
    title="직무를 삭제할까요?"
    :subject="deleteTarget.name"
    detail=" 직무 기록이 영구 삭제됩니다. 공고의 마지막 직무는 지울 수 없습니다."
    aria-label="직무 삭제 확인"
    :busy="deleting"
    @confirm="remove"
    @cancel="deleteTarget = null"
  />
</template>
