<script setup lang="ts">
import { reactive, watch } from 'vue'
import type { Company, CompanyPayload, CompanyStatus } from '../types/company'

const props = defineProps<{
  company: Company | null
  saving: boolean
  apiFieldErrors: Record<string, string>
}>()

const emit = defineEmits<{
  submit: [payload: CompanyPayload]
  cancel: []
}>()

const emptyForm = (): CompanyPayload => ({
  name: '', industry: '', location: '', websiteUrl: '', status: 'INTERESTED', summary: '', memo: '',
})

const form = reactive<CompanyPayload>(emptyForm())
const localErrors = reactive<Record<string, string>>({})

watch(
  () => props.company,
  (company) => {
    Object.assign(form, company
      ? {
          name: company.name,
          industry: company.industry ?? '',
          location: company.location ?? '',
          websiteUrl: company.websiteUrl ?? '',
          status: company.status,
          summary: company.summary ?? '',
          memo: company.memo ?? '',
        }
      : emptyForm())
    Object.keys(localErrors).forEach((key) => delete localErrors[key])
  },
  { immediate: true },
)

const fieldError = (name: string) => localErrors[name] ?? props.apiFieldErrors[name]

function validate() {
  Object.keys(localErrors).forEach((key) => delete localErrors[key])
  if (!form.name.trim()) localErrors.name = '기업명을 입력해 주세요.'
  if (form.name.length > 120) localErrors.name = '기업명은 120자 이하여야 합니다.'
  if (form.websiteUrl && !/^https?:\/\/.+/i.test(form.websiteUrl)) {
    localErrors.websiteUrl = 'http 또는 https URL을 입력해 주세요.'
  }
  return Object.keys(localErrors).length === 0
}

function submit() {
  if (!validate()) return
  emit('submit', { ...form, name: form.name.trim() })
}

const statuses: Array<{ value: CompanyStatus; label: string }> = [
  { value: 'INTERESTED', label: '관심' },
  { value: 'PREPARING', label: '지원 준비' },
  { value: 'APPLIED', label: '지원 완료' },
  { value: 'ARCHIVED', label: '보관' },
]
</script>

<template>
  <div class="modal-backdrop" @mousedown.self="emit('cancel')">
    <section class="modal-card" role="dialog" aria-modal="true" :aria-label="company ? '기업 정보 수정' : '기업 추가'">
      <div class="modal-head">
        <div>
          <p class="eyebrow">{{ company ? '정보 업데이트' : '새 기업 기록' }}</p>
          <h2>{{ company ? '기업 정보 수정' : '기업 추가' }}</h2>
        </div>
        <button type="button" class="icon-button" aria-label="닫기" @click="emit('cancel')">×</button>
      </div>
      <form @submit.prevent="submit">
        <div class="form-grid">
          <label class="field full">
            <span>기업명 <b>*</b></span>
            <input v-model="form.name" maxlength="120" autofocus />
            <small v-if="fieldError('name')" class="field-error">{{ fieldError('name') }}</small>
          </label>
          <label class="field">
            <span>산업 분야</span>
            <input v-model="form.industry" maxlength="120" placeholder="예: 산업용 로봇" />
          </label>
          <label class="field">
            <span>지역</span>
            <input v-model="form.location" maxlength="160" placeholder="예: 경기 성남" />
          </label>
          <label class="field">
            <span>지원 상태</span>
            <select v-model="form.status">
              <option v-for="status in statuses" :key="status.value" :value="status.value">{{ status.label }}</option>
            </select>
          </label>
          <label class="field">
            <span>웹사이트</span>
            <input v-model="form.websiteUrl" maxlength="500" placeholder="https://" />
            <small v-if="fieldError('websiteUrl')" class="field-error">{{ fieldError('websiteUrl') }}</small>
          </label>
          <label class="field full">
            <span>핵심 요약</span>
            <textarea v-model="form.summary" maxlength="2000" rows="3" placeholder="기업과 직무를 빠르게 파악할 수 있는 내용을 기록하세요." />
          </label>
          <label class="field full">
            <span>지원 메모</span>
            <textarea v-model="form.memo" maxlength="5000" rows="5" placeholder="연결할 경험, 확인할 질문, 다음 행동을 기록하세요." />
          </label>
        </div>
        <div class="modal-actions">
          <button type="button" class="button secondary" :disabled="saving" @click="emit('cancel')">취소</button>
          <button type="submit" class="button primary" :disabled="saving">{{ saving ? '저장 중…' : '저장' }}</button>
        </div>
      </form>
    </section>
  </div>
</template>
