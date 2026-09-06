<script setup lang="ts">
import { reactive, watch } from 'vue'
import type { Company } from '../types/company'
import type { ApplicationStatus, EmploymentType, JobPosting, JobPostingPayload, StepResult } from '../types/posting'
import { employmentTypeLabels, nowLocalInput, selectableStatuses, statusLabels, toLocalInput, toUtcIso } from '../types/posting'

const props = defineProps<{
  posting: JobPosting | null
  companies: Company[]
  saving: boolean
  apiFieldErrors: Record<string, string>
}>()

const emit = defineEmits<{
  submit: [payload: JobPostingPayload]
}>()

interface StepRow { name: string; result: StepResult; scheduledLocal: string; memo: string }
interface PositionRow { id: string | null; name: string }

/** deadline/scheduled 은 datetime-local 로 다룬다. 나머지는 페이로드 그대로. */
interface FormState {
  companyId: string | null
  companyName: string
  title: string
  postingUrl: string
  employmentType: EmploymentType
  deadlineLocal: string
  status: ApplicationStatus
  qualifications: string
  positions: PositionRow[]
  steps: StepRow[]
}

const emptyForm = (): FormState => ({
  companyId: null, companyName: '', title: '', postingUrl: '',
  employmentType: 'FULL_TIME', deadlineLocal: nowLocalInput(), status: 'INTERESTED', qualifications: '',
  positions: [], steps: [],
})

const form = reactive<FormState>(emptyForm())
const localErrors = reactive<Record<string, string>>({})

watch(
  () => props.posting,
  (posting) => {
    Object.assign(form, posting
      ? {
          companyId: posting.companyId,
          companyName: '',
          title: posting.title,
          postingUrl: posting.postingUrl ?? '',
          employmentType: posting.employmentType,
          deadlineLocal: toLocalInput(posting.deadlineAt),
          status: posting.status,
          qualifications: posting.qualifications ?? '',
          positions: posting.positions.map((p) => ({ id: p.id, name: p.name })),
          steps: posting.steps.map((s) => ({
            name: s.name, result: s.result, scheduledLocal: toLocalInput(s.scheduledAt), memo: s.memo ?? '',
          })),
        }
      : emptyForm())
    Object.keys(localErrors).forEach((key) => delete localErrors[key])
  },
  { immediate: true },
)

const fieldError = (name: string) => localErrors[name] ?? props.apiFieldErrors[name]

function addPosition() {
  form.positions.push({ id: null, name: '' })
}
function removePosition(index: number) {
  form.positions.splice(index, 1)
}
function addStep() {
  form.steps.push({ name: '', result: 'UPCOMING', scheduledLocal: nowLocalInput(), memo: '' })
}
function removeStep(index: number) {
  form.steps.splice(index, 1)
}
function moveStep(index: number, delta: number) {
  const target = index + delta
  if (target < 0 || target >= form.steps.length) return
  const [row] = form.steps.splice(index, 1)
  form.steps.splice(target, 0, row)
}

function validate() {
  Object.keys(localErrors).forEach((key) => delete localErrors[key])
  if (!form.title.trim()) localErrors.title = '모집 부문을 입력해 주세요.'
  if (!form.companyId && !form.companyName.trim()) {
    localErrors.companyName = '등록된 기업을 고르거나 회사명을 입력해 주세요.'
  }
  if (form.postingUrl && !/^https?:\/\/.+/i.test(form.postingUrl)) {
    localErrors.postingUrl = 'http 또는 https URL을 입력해 주세요.'
  }
  if (form.steps.some((step) => !step.name.trim())) localErrors.steps = '단계 이름을 모두 채워 주세요.'
  return Object.keys(localErrors).length === 0
}

function submit() {
  if (!validate()) return
  emit('submit', {
    companyId: form.companyId,
    companyName: form.companyId ? '' : form.companyName.trim(),
    title: form.title.trim(),
    postingUrl: form.postingUrl.trim(),
    employmentType: form.employmentType,
    deadlineAt: toUtcIso(form.deadlineLocal),
    status: form.status,
    qualifications: form.qualifications,
    // 빈 이름은 버린다. 다 비면 서버가 제목 이름의 직무 하나를 만든다.
    positions: form.positions
      .filter((p) => p.name.trim())
      .map((p) => ({ id: p.id, name: p.name.trim() })),
    steps: form.steps.map((s) => ({
      name: s.name.trim(), result: s.result, scheduledAt: toUtcIso(s.scheduledLocal), memo: s.memo,
    })),
  })
}

const employmentTypes = Object.entries(employmentTypeLabels) as Array<[EmploymentType, string]>
const statuses = selectableStatuses.map((status) => [status, statusLabels[status]] as const)
</script>

<template>
  <!-- 껍데기는 Drawer 가 가진다. 저장 버튼은 드로어 헤더에서 form="posting-form" 으로 제출한다. -->
  <form id="posting-form" @submit.prevent="submit">
    <div class="form-grid">
      <label class="field">
        <span>고용회사 <small>등록된 기업</small></span>
        <select v-model="form.companyId">
          <option :value="null">직접 입력</option>
          <option v-for="company in companies" :key="company.id" :value="company.id">{{ company.name }}</option>
        </select>
      </label>
      <label class="field">
        <span>회사명 <small v-if="!form.companyId">없으면 기업이 새로 만들어집니다</small></span>
        <input
          v-model="form.companyName"
          maxlength="120"
          :disabled="form.companyId !== null"
          :placeholder="form.companyId ? '선택한 기업 이름을 씁니다' : '예: 세렌디스'"
        />
        <small v-if="fieldError('companyName')" class="field-error">{{ fieldError('companyName') }}</small>
      </label>
      <label class="field full">
        <span>모집 부문 <b>*</b> <small>공채 이름 또는 단일 직무명</small></span>
        <input v-model="form.title" maxlength="160" autofocus placeholder="예: 2026 상반기 신입 공채 / AI 개발자 채용" />
        <small v-if="fieldError('title')" class="field-error">{{ fieldError('title') }}</small>
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
        <span>내 상태</span>
        <select v-model="form.status">
          <option v-for="[value, label] in statuses" :key="value" :value="value">{{ label }}</option>
        </select>
      </label>
      <label class="field full">
        <span>서류마감 <small>비우면 상시</small></span>
        <input v-model="form.deadlineLocal" type="datetime-local" />
      </label>

      <p class="form-section">모집 직무 <small>비우면 모집 부문 이름으로 하나 생깁니다. 상세는 모집 직무 탭에서.</small></p>
      <div class="repeater full">
        <div v-for="(row, index) in form.positions" :key="index" class="repeater__row">
          <input v-model="row.name" maxlength="160" placeholder="예: 백엔드 개발" />
          <button type="button" class="icon-button" aria-label="직무 삭제" @click="removePosition(index)">×</button>
        </div>
        <button type="button" class="button ghost compact" @click="addPosition">+ 직무 추가</button>
      </div>

      <p class="form-section">채용 절차 <small>위에서부터 순서대로</small></p>
      <div class="repeater full">
        <div v-for="(row, index) in form.steps" :key="index" class="repeater__row repeater__row--step">
          <span class="repeater__seq">{{ index + 1 }}</span>
          <input v-model="row.name" maxlength="60" placeholder="예: 서류 / 인적성 / 코딩테스트 / 면접" />
          <input v-model="row.scheduledLocal" type="datetime-local" title="일정(선택)" />
          <button type="button" class="icon-button" aria-label="위로" :disabled="index === 0" @click="moveStep(index, -1)">↑</button>
          <button type="button" class="icon-button" aria-label="아래로" :disabled="index === form.steps.length - 1" @click="moveStep(index, 1)">↓</button>
          <button type="button" class="icon-button" aria-label="단계 삭제" @click="removeStep(index)">×</button>
        </div>
        <small v-if="fieldError('steps')" class="field-error">{{ fieldError('steps') }}</small>
        <button type="button" class="button ghost compact" @click="addStep">+ 단계 추가</button>
      </div>

      <label class="field full">
        <span>지원자격</span>
        <textarea v-model="form.qualifications" rows="3" placeholder="학력, 경력, 필수 조건" />
      </label>
    </div>
  </form>
</template>
