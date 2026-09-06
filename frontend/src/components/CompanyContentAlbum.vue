<script setup lang="ts">
import { onMounted, ref } from 'vue'
import {
  ApiClientError,
  createCompanyContent,
  deleteCompanyContent,
  listCompanyContents,
  updateCompanyContent,
} from '../api/company-contents'
import type { CompanyContent, CompanyContentPayload } from '../types/company-content'
import { companyContentKindLabels } from '../types/company-content'
import CompanyContentForm from './CompanyContentForm.vue'
import ConfirmDialog from './ConfirmDialog.vue'

const props = defineProps<{ companyId: string }>()

const contents = ref<CompanyContent[]>([])
const loading = ref(true)
const errorMessage = ref('')
const notice = ref('')
const editing = ref<CompanyContent | null>(null)
const formOpen = ref(false)
const saving = ref(false)
const fieldErrors = ref<Record<string, string>>({})
const deleteTarget = ref<CompanyContent | null>(null)
const deleting = ref(false)

async function load() {
  loading.value = true
  errorMessage.value = ''
  try {
    contents.value = await listCompanyContents(props.companyId)
  } catch (error) {
    errorMessage.value = error instanceof Error ? error.message : '자료를 불러오지 못했습니다.'
  } finally {
    loading.value = false
  }
}

function openForm(content: CompanyContent | null = null) {
  editing.value = content
  fieldErrors.value = {}
  formOpen.value = true
}

function closeForm() {
  formOpen.value = false
  editing.value = null
}

async function save(payload: CompanyContentPayload) {
  saving.value = true
  fieldErrors.value = {}
  errorMessage.value = ''
  try {
    const saved = editing.value
      ? await updateCompanyContent(props.companyId, editing.value.id, payload)
      : await createCompanyContent(props.companyId, payload)
    const oldIndex = contents.value.findIndex((content) => content.id === saved.id)
    if (oldIndex >= 0) contents.value.splice(oldIndex, 1)
    contents.value.unshift(saved)
    notice.value = editing.value ? '자료를 수정했습니다.' : '자료를 추가했습니다.'
    closeForm()
  } catch (error) {
    if (error instanceof ApiClientError) {
      fieldErrors.value = error.fieldErrors
      if (!Object.keys(error.fieldErrors).length) errorMessage.value = error.message
    } else {
      errorMessage.value = '저장 중 오류가 발생했습니다.'
    }
  } finally {
    saving.value = false
  }
}

async function remove() {
  if (!deleteTarget.value) return
  deleting.value = true
  errorMessage.value = ''
  try {
    await deleteCompanyContent(props.companyId, deleteTarget.value.id)
    contents.value = contents.value.filter((content) => content.id !== deleteTarget.value?.id)
    deleteTarget.value = null
    notice.value = '자료를 삭제했습니다.'
  } catch (error) {
    errorMessage.value = error instanceof Error ? error.message : '삭제 중 오류가 발생했습니다.'
  } finally {
    deleting.value = false
  }
}

onMounted(load)
</script>

<template>
  <section class="content-album">
    <div class="content-album__head">
      <span class="label">저장한 자료 · {{ contents.length }}건</span>
      <button v-if="!formOpen" type="button" class="button primary compact" @click="openForm()">자료 추가</button>
    </div>

    <div v-if="notice" class="notice success" role="status">{{ notice }}</div>
    <div v-if="errorMessage" class="notice error" role="alert">
      <span>{{ errorMessage }}</span>
      <button v-if="contents.length === 0" type="button" @click="load">다시 시도</button>
    </div>

    <CompanyContentForm
      v-if="formOpen"
      :content="editing"
      :saving="saving"
      :api-field-errors="fieldErrors"
      @submit="save"
      @cancel="closeForm"
    />

    <p v-if="loading" class="body-copy empty" aria-live="polite">자료를 불러오는 중입니다.</p>
    <p v-else-if="contents.length === 0 && !formOpen" class="body-copy empty">저장한 뉴스나 유튜브가 없습니다.</p>
    <div v-else class="content-grid">
      <article v-for="content in contents" :key="content.id" class="content-card">
        <div class="content-card__head">
          <span class="chip" :data-tone="content.kind === 'YOUTUBE' ? 'negative' : 'primary'">
            {{ companyContentKindLabels[content.kind] }}
          </span>
          <span v-if="content.source" class="content-card__source">{{ content.source }}</span>
        </div>
        <strong>{{ content.title }}</strong>
        <p class="content-card__preview">{{ content.preview || '미리보기 내용이 없습니다.' }}</p>
        <div class="content-card__foot">
          <a :href="content.url" target="_blank" rel="noreferrer">링크 열기 ↗</a>
          <span class="content-card__tools">
            <button type="button" @click="openForm(content)">수정</button>
            <button type="button" class="danger" @click="deleteTarget = content">삭제</button>
          </span>
        </div>
      </article>
    </div>
  </section>

  <ConfirmDialog
    v-if="deleteTarget"
    title="이 자료를 삭제할까요?"
    :subject="deleteTarget.title"
    detail="은 기업의 뉴스·유튜브 앨범에서 삭제됩니다."
    aria-label="기업 자료 삭제 확인"
    :busy="deleting"
    @confirm="remove"
    @cancel="deleteTarget = null"
  />
</template>
