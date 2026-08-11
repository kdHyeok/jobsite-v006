<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { ApiClientError, createCompany, deleteCompany, listCompanies, updateCompany } from './api/companies'
import CompanyDetail from './components/CompanyDetail.vue'
import CompanyForm from './components/CompanyForm.vue'
import CompanyList from './components/CompanyList.vue'
import ConfirmDialog from './components/ConfirmDialog.vue'
import type { Company, CompanyPayload } from './types/company'

const companies = ref<Company[]>([])
const selectedId = ref<string | null>(null)
const loading = ref(true)
const loadError = ref('')
const notice = ref('')
const formOpen = ref(false)
const editingCompany = ref<Company | null>(null)
const saving = ref(false)
const formErrors = ref<Record<string, string>>({})
const deleteTarget = ref<Company | null>(null)
const deleting = ref(false)

const selectedCompany = computed(() =>
  companies.value.find((company) => company.id === selectedId.value) ?? null,
)

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
    const saved = editingCompany.value
      ? await updateCompany(editingCompany.value.id, payload)
      : await createCompany(payload)
    await load()
    selectedId.value = saved.id
    formOpen.value = false
    notice.value = editingCompany.value ? '기업 정보를 수정했습니다.' : '새 기업을 추가했습니다.'
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
    await load()
    notice.value = '기업 정보를 삭제했습니다.'
  } catch (error) {
    loadError.value = error instanceof Error ? error.message : '삭제 중 오류가 발생했습니다.'
  } finally {
    deleting.value = false
  }
}

onMounted(load)
</script>

<template>
  <div class="app-shell">
    <header class="topbar">
      <div class="brand">
        <span class="brand-mark">J</span>
        <div>
          <strong>JobSight</strong>
          <span>기업 정보 워크스페이스</span>
        </div>
      </div>
      <div class="topbar-actions">
        <span class="demo-badge">합성 데모 데이터</span>
        <button type="button" class="button primary" @click="openCreate">+ 기업 추가</button>
      </div>
    </header>

    <main class="workspace">
      <section class="hero">
        <div>
          <p class="eyebrow">COMPANY RESEARCH MVP · v0.0.6</p>
          <h1>지원할 기업을 한 화면에서 정리하세요.</h1>
          <p>관심도, 핵심 정보, 개인 메모를 PostgreSQL에 안전하게 저장하는 최소 CRUD 워크스페이스입니다.</p>
        </div>
        <div class="metric-card">
          <strong>{{ companies.length }}</strong>
          <span>정리한 기업</span>
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
      :company-name="deleteTarget.name"
      :deleting="deleting"
      @confirm="remove"
      @cancel="deleteTarget = null"
    />
  </div>
</template>
