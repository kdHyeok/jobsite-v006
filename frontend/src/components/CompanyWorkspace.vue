<script setup lang="ts">
import { onMounted, ref, watch } from 'vue'
import {
  ApiClientError,
  createCompany,
  deleteCompany,
  getCompany,
  listCompanies,
  updateCompany,
} from '../api/companies'
import CompanyDetail from './CompanyDetail.vue'
import CompanyForm from './CompanyForm.vue'
import CompanyList from './CompanyList.vue'
import ConfirmDialog from './ConfirmDialog.vue'
import type { Company, CompanyPayload } from '../types/company'

const companies = ref<Company[]>([])
const selectedId = ref<string | null>(null)
/** 목록 응답에는 openPostings 가 없다. 선택한 기업만 상세로 다시 읽는다. */
const selectedCompany = ref<Company | null>(null)
const loading = ref(true)
const loadError = ref('')
const notice = ref('')
const formOpen = ref(false)
const editingCompany = ref<Company | null>(null)
const saving = ref(false)
const formErrors = ref<Record<string, string>>({})
const deleteTarget = ref<Company | null>(null)
const deleting = ref(false)

async function load() {
  loading.value = true
  loadError.value = ''
  try {
    companies.value = await listCompanies()
    if (!companies.value.some((company) => company.id === selectedId.value)) {
      selectedId.value = companies.value[0]?.id ?? null
    }
  } catch (error) {
    loadError.value = error instanceof Error ? error.message : '기업 목록을 불러오지 못했습니다.'
  } finally {
    loading.value = false
  }
}

async function loadSelected(id: string | null) {
  if (!id) {
    selectedCompany.value = null
    return
  }
  try {
    selectedCompany.value = await getCompany(id)
  } catch (error) {
    selectedCompany.value = null
    loadError.value = error instanceof Error ? error.message : '기업 상세를 불러오지 못했습니다.'
  }
}

function openCreate() {
  editingCompany.value = null
  formErrors.value = {}
  formOpen.value = true
}

function openEdit() {
  if (!selectedCompany.value) return
  editingCompany.value = selectedCompany.value
  formErrors.value = {}
  formOpen.value = true
}

async function save(payload: CompanyPayload) {
  saving.value = true
  formErrors.value = {}
  notice.value = ''
  try {
    const editingId = editingCompany.value?.id ?? null
    const saved = editingId
      ? await updateCompany(editingId, payload)
      : await createCompany(payload)
    formOpen.value = false
    await load()
    if (selectedId.value === saved.id) await loadSelected(saved.id)
    else selectedId.value = saved.id
    notice.value = editingId ? '기업 정보를 수정했습니다.' : '새 기업을 추가했습니다.'
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

async function remove() {
  if (!deleteTarget.value) return
  deleting.value = true
  notice.value = ''
  try {
    await deleteCompany(deleteTarget.value.id)
    deleteTarget.value = null
    selectedId.value = null
    await load()
    notice.value = '기업 정보를 삭제했습니다.'
  } catch (error) {
    loadError.value = error instanceof Error ? error.message : '삭제 중 오류가 발생했습니다.'
  } finally {
    deleting.value = false
  }
}

watch(selectedId, loadSelected)

onMounted(load)
</script>

<template>
  <main class="workspace">
    <section class="hero">
      <div>
        <p class="eyebrow">COMPANY RESEARCH MVP · v0.0.6</p>
        <h1>지원할 기업을 한 화면에서 정리하세요.</h1>
        <p>기업 정보와 진행 중인 채용공고를 내 계정에만 저장하는 워크스페이스입니다.</p>
      </div>
      <div class="hero-aside">
        <div class="metric-card">
          <strong>{{ companies.length }}</strong>
          <span>정리한 기업</span>
        </div>
        <button type="button" class="button primary" @click="openCreate">+ 기업 추가</button>
      </div>
    </section>

    <div v-if="notice" class="notice success" role="status">{{ notice }}</div>
    <div v-if="loadError" class="notice error" role="alert">
      <span>{{ loadError }}</span>
      <button type="button" @click="load">다시 시도</button>
    </div>

    <section v-if="loading" class="loading-card" aria-live="polite">
      <span class="spinner" /> 기업 정보를 불러오는 중입니다.
    </section>

    <section v-else class="content-grid">
      <CompanyList
        :companies="companies"
        :selected-id="selectedId"
        @select="selectedId = $event.id"
      />
      <CompanyDetail
        v-if="selectedCompany"
        :company="selectedCompany"
        @edit="openEdit"
        @delete="deleteTarget = selectedCompany"
      />
      <div v-else class="empty-state detail-empty">
        <span class="empty-icon">＋</span>
        <strong>기업을 선택하거나 추가하세요.</strong>
        <p>목록에서 기업을 선택하면 상세 정보가 표시됩니다.</p>
        <button type="button" class="button primary" @click="openCreate">첫 기업 추가</button>
      </div>
    </section>
  </main>

  <CompanyForm
    v-if="formOpen"
    :company="editingCompany"
    :saving="saving"
    :api-field-errors="formErrors"
    @submit="save"
    @cancel="formOpen = false"
  />
  <ConfirmDialog
    v-if="deleteTarget"
    title="기업 정보를 삭제할까요?"
    :subject="deleteTarget.name"
    detail="의 기록과 연결된 채용공고가 함께 삭제됩니다."
    aria-label="기업 삭제 확인"
    :busy="deleting"
    @confirm="remove"
    @cancel="deleteTarget = null"
  />
</template>
