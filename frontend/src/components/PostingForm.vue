<script setup lang="ts">
import { reactive, watch } from 'vue'
import type { Company } from '../types/company'
import type { ApplicationStage, EmploymentType, JobPosting, JobPostingPayload } from '../types/posting'
import { employmentTypeLabels, stageLabels, toLocalInput, toUtcIso } from '../types/posting'

const props = defineProps<{
  posting: JobPosting | null
  companies: Company[]
  saving: boolean
  apiFieldErrors: Record<string, string>
}>()

const emit = defineEmits<{
  submit: [payload: JobPostingPayload]
  cancel: []
}>()

/** deadlineAt 만 화면 표현이 다르다(datetime-local). 나머지는 페이로드 그대로. */
type FormState = Omit<JobPostingPayload, 'deadlineAt'> & { deadlineLocal: string }

const emptyForm = (): FormState => ({
  companyId: null, companyName: '', position: '', postingUrl: '',
  employmentType: 'FULL_TIME', deadlineLocal: '', stage: 'INTERESTED',
  headcount: '', workLocation: '', qualifications: '', responsibilities: '', requiredSkills: '',
})

const form = reactive<FormState>(emptyForm())
const localErrors = reactive<Record<string, string>>({})

watch(
  () => props.posting,
  (posting) => {
    Object.assign(form, posting
      ? {
          companyId: posting.companyId,
          companyName: posting.companyId ? '' : (posting.companyName ?? ''),
          position: posting.position,
          postingUrl: posting.postingUrl ?? '',
          employmentType: posting.employmentType,
          deadlineLocal: toLocalInput(posting.deadlineAt),
          stage: posting.stage,
          headcount: posting.headcount ?? '',
          workLocation: posting.workLocation ?? '',
          qualifications: posting.qualifications ?? '',
          responsibilities: posting.responsibilities ?? '',
          requiredSkills: posting.requiredSkills ?? '',
        }
      : emptyForm())
    Object.keys(localErrors).forEach((key) => delete localErrors[key])
  },
  { immediate: true },
)

const fieldError = (name: string) => localErrors[name] ?? props.apiFieldErrors[name]

function validate() {
  Object.keys(localErrors).forEach((key) => delete localErrors[key])
  if (!form.position.trim()) localErrors.position = '채용직무를 입력해 주세요.'
  if (!form.companyId && !form.companyName.trim()) {
    localErrors.companyName = '등록된 기업을 고르거나 회사명을 입력해 주세요.'
  }
  if (form.postingUrl && !/^https?:\/\/.+/i.test(form.postingUrl)) {
    localErrors.postingUrl = 'http 또는 https URL을 입력해 주세요.'
  }
  return Object.keys(localErrors).length === 0
}

function submit() {
  if (!validate()) return
  emit('submit', {
    ...form,
    position: form.position.trim(),
    // 기업을 연결하면 이름은 서버가 기업에서 가져온다.
    companyName: form.companyId ? '' : form.companyName.trim(),
    postingUrl: form.postingUrl.trim(),
    deadlineAt: toUtcIso(form.deadlineLocal),
  })
}

const employmentTypes = Object.entries(employmentTypeLabels) as Array<[EmploymentType, string]>
const stages = Object.entries(stageLabels) as Array<[ApplicationStage, string]>
</script>

<template>
  <div class="modal-backdrop" @mousedown.self="emit('cancel')">
    <section class="modal-card" role="dialog" aria-modal="true" :aria-label="posting ? '채용공고 수정' : '채용공고 추가'">
      <div class="modal-head">
        <div>
          <p class="eyebrow">{{ posting ? '공고 업데이트' : '새 공고 기록' }}</p>
          <h2>{{ posting ? '채용공고 수정' : '채용공고 추가' }}</h2>
        </div>
        <button type="button" class="icon-button" aria-label="닫기" @click="emit('cancel')">×</button>
      </div>
      <form @submit.prevent="submit">
        <div class="form-grid">
          <label class="field">
            <span>고용회사 <small>등록된 기업</small></span>
            <select v-model="form.companyId">
              <option :value="null">직접 입력</option>
              <option v-for="company in companies" :key="company.id" :value="company.id">{{ company.name }}</option>
            </select>
          </label>
          <label class="field">
            <span>회사명 <small v-if="!form.companyId">직접 입력</small></span>
            <input
              v-model="form.companyName"
              maxlength="120"
              :disabled="form.companyId !== null"
              :placeholder="form.companyId ? '선택한 기업 이름을 씁니다' : '예: 세렌디스'"
            />
            <small v-if="fieldError('companyName')" class="field-error">{{ fieldError('companyName') }}</small>
          </label>
          <label class="field full">
            <span>채용직무 <b>*</b></span>
            <input v-model="form.position" maxlength="160" autofocus placeholder="예: 백엔드 엔지니어 (신입)" />
            <small v-if="fieldError('position')" class="field-error">{{ fieldError('position') }}</small>
          </label>
          <label class="field full">
            <span>공고링크</span>
            <input v-model="form.postingUrl" maxlength="500" placeholder="https://" />
            <small v-if="fieldError('postingUrl')" class="field-error">{{ fieldError('postingUrl') }}</small>
          </label>
          <label class="field">
            <span>고용형태</span>
            <select v-model="form.employmentType">
              <option v-for="[value, label] in employmentTypes" :key="value" :value="value">{{ label }}</option>
            </select>
          </label>
          <label class="field">
            <span>지원단계</span>
            <select v-model="form.stage">
              <option v-for="[value, label] in stages" :key="value" :value="value">{{ label }}</option>
            </select>
          </label>
          <label class="field">
            <span>서류마감 <small>비우면 상시</small></span>
            <input v-model="form.deadlineLocal" type="datetime-local" />
          </label>
          <label class="field">
            <span>모집인원</span>
            <input v-model="form.headcount" maxlength="60" placeholder="예: 0명 (수시)" />
          </label>
          <label class="field full">
            <span>근무지역</span>
            <input v-model="form.workLocation" maxlength="160" placeholder="예: 서울 강남구 · 주 2회 재택" />
          </label>
          <label class="field full">
            <span>지원자격</span>
            <textarea v-model="form.qualifications" rows="3" placeholder="학력, 경력, 필수 조건을 기록하세요." />
          </label>
          <label class="field full">
            <span>담당업무</span>
            <textarea v-model="form.responsibilities" rows="3" placeholder="맡게 될 일을 기록하세요." />
          </label>
          <label class="field full">
            <span>요구역량</span>
            <textarea v-model="form.requiredSkills" rows="3" placeholder="예: Java, Spring, RDB 설계 경험" />
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
