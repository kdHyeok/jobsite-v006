<script setup lang="ts">
import { reactive, watch } from 'vue'
import type { Company, CompanyPayload, CompanySize } from '../types/company'
import { companySizeLabels } from '../types/company'

const props = defineProps<{
  company: Company | null
  saving: boolean
  apiFieldErrors: Record<string, string>
}>()

const emit = defineEmits<{
  submit: [payload: CompanyPayload]
  cancel: []
}>()

/** 업종은 쉼표로 나눠 받는다. 칩 에디터를 따로 만들 만큼 자주 쓰이지 않는다. */
type FormState = Omit<CompanyPayload, 'industries' | 'annualRevenue' | 'employeeCount'> & {
  industriesText: string
  annualRevenueText: string
  employeeCountText: string
}

const emptyForm = (): FormState => ({
  name: '', websiteUrl: '', industriesText: '', companySize: null,
  annualRevenueText: '', employeeCountText: '', address: '',
  foundedOn: '', summary: '', memo: '',
})

const form = reactive<FormState>(emptyForm())
const localErrors = reactive<Record<string, string>>({})

watch(
  () => props.company,
  (company) => {
    Object.assign(form, company
      ? {
          name: company.name,
          websiteUrl: company.websiteUrl ?? '',
          industriesText: company.industries.join(', '),
          companySize: company.companySize,
          annualRevenueText: company.annualRevenue === null ? '' : String(company.annualRevenue),
          employeeCountText: company.employeeCount === null ? '' : String(company.employeeCount),
          address: company.address ?? '',
          // input[type=month] 는 YYYY-MM 만 받는다. 저장은 그 달 1일로 한다.
          foundedOn: company.foundedOn ? company.foundedOn.slice(0, 7) : '',
          summary: company.summary ?? '',
          memo: company.memo ?? '',
        }
      : emptyForm())
    Object.keys(localErrors).forEach((key) => delete localErrors[key])
  },
  { immediate: true },
)

const fieldError = (name: string) => localErrors[name] ?? props.apiFieldErrors[name]

const parseNumber = (text: string) => (text.trim() === '' ? null : Number(text))

function validate() {
  Object.keys(localErrors).forEach((key) => delete localErrors[key])
  if (!form.name.trim()) localErrors.name = '기업명을 입력해 주세요.'
  else if (form.name.length > 120) localErrors.name = '기업명은 120자 이하여야 합니다.'
  if (form.websiteUrl && !/^https?:\/\/.+/i.test(form.websiteUrl)) {
    localErrors.websiteUrl = 'http 또는 https URL을 입력해 주세요.'
  }
  const revenue = parseNumber(form.annualRevenueText)
  if (revenue !== null && (!Number.isFinite(revenue) || revenue < 0)) {
    localErrors.annualRevenue = '0 이상 숫자를 입력해 주세요.'
  }
  const employees = parseNumber(form.employeeCountText)
  if (employees !== null && (!Number.isInteger(employees) || employees < 0)) {
    localErrors.employeeCount = '0 이상 정수를 입력해 주세요.'
  }
  return Object.keys(localErrors).length === 0
}

function submit() {
  if (!validate()) return
  const industries = [...new Set(
    form.industriesText.split(',').map((value) => value.trim()).filter(Boolean),
  )]
  emit('submit', {
    name: form.name.trim(),
    websiteUrl: form.websiteUrl.trim(),
    industries,
    companySize: form.companySize,
    annualRevenue: parseNumber(form.annualRevenueText),
    employeeCount: parseNumber(form.employeeCountText),
    address: form.address.trim(),
    foundedOn: form.foundedOn ? `${form.foundedOn}-01` : null,
    summary: form.summary,
    memo: form.memo,
  })
}

const sizes = Object.entries(companySizeLabels) as Array<[CompanySize, string]>
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
          <label class="field full">
            <span>기업 홈페이지</span>
            <input v-model="form.websiteUrl" maxlength="500" placeholder="https://" />
            <small v-if="fieldError('websiteUrl')" class="field-error">{{ fieldError('websiteUrl') }}</small>
          </label>
          <label class="field full">
            <span>간략 소개</span>
            <textarea v-model="form.summary" maxlength="2000" rows="3" placeholder="기업과 직무를 빠르게 파악할 수 있는 내용을 기록하세요." />
          </label>

          <p class="form-section full">기업 정보</p>
          <label class="field full">
            <span>업종 <small>쉼표로 여러 개</small></span>
            <input v-model="form.industriesText" list="industry-suggestions" placeholder="예: IT서비스, 금융권, SI" />
            <datalist id="industry-suggestions">
              <option value="IT서비스" />
              <option value="SI" />
              <option value="금융권" />
              <option value="게임" />
              <option value="이커머스" />
              <option value="제조" />
              <option value="바이오·헬스케어" />
              <option value="교육" />
            </datalist>
            <small v-if="fieldError('industries')" class="field-error">{{ fieldError('industries') }}</small>
          </label>
          <label class="field">
            <span>기업 형태</span>
            <select v-model="form.companySize">
              <option :value="null">선택 안 함</option>
              <option v-for="[value, label] in sizes" :key="value" :value="value">{{ label }}</option>
            </select>
          </label>
          <label class="field">
            <span>설립연월</span>
            <input v-model="form.foundedOn" type="month" />
          </label>
          <label class="field">
            <span>매출액 <small>원 단위</small></span>
            <input v-model="form.annualRevenueText" type="number" min="0" step="1" placeholder="예: 120000000000" />
            <small v-if="fieldError('annualRevenue')" class="field-error">{{ fieldError('annualRevenue') }}</small>
          </label>
          <label class="field">
            <span>사원수</span>
            <input v-model="form.employeeCountText" type="number" min="0" step="1" placeholder="예: 420" />
            <small v-if="fieldError('employeeCount')" class="field-error">{{ fieldError('employeeCount') }}</small>
          </label>
          <label class="field full">
            <span>주소</span>
            <input v-model="form.address" maxlength="200" placeholder="예: 경기 성남시 분당구 …" />
            <small v-if="fieldError('address')" class="field-error">{{ fieldError('address') }}</small>
          </label>

          <p class="form-section full">개인 기록</p>
          <label class="field full">
            <span>지원 메모</span>
            <textarea v-model="form.memo" maxlength="5000" rows="4" placeholder="연결할 경험, 확인할 질문, 다음 행동을 기록하세요." />
          </label>
        </div>
        <p class="form-hint">채용정보는 채용공고를 등록하면 자동으로 채워집니다.</p>
        <div class="modal-actions">
          <button type="button" class="button secondary" :disabled="saving" @click="emit('cancel')">취소</button>
          <button type="submit" class="button primary" :disabled="saving">{{ saving ? '저장 중…' : '저장' }}</button>
        </div>
      </form>
    </section>
  </div>
</template>
