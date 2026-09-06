<script setup lang="ts">
import { computed, reactive, watch } from 'vue'
import type { Company, CompanyPayload, CompanySize, RevenueUnit } from '../types/company'
import { companySizeLabels } from '../types/company'
import TagInput from './TagInput.vue'

const props = defineProps<{
  company: Company | null
  saving: boolean
  apiFieldErrors: Record<string, string>
  industryOptions: string[]
}>()

const emit = defineEmits<{
  submit: [payload: CompanyPayload]
  cancel: []
}>()

type FormState = Omit<CompanyPayload, 'annualRevenue' | 'employeeCount' | 'revenueUnit'> & {
  revenueUnit: RevenueUnit
  annualRevenueText: string
  employeeCountText: string
}

const emptyForm = (): FormState => ({
  name: '', websiteUrl: '', industries: [], companySize: null,
  revenueUnit: 'TEN_THOUSAND',
  annualRevenueText: '', employeeCountText: '', address: '',
  foundedOn: '', summary: '', benefits: '', memo: '',
})

const form = reactive<FormState>(emptyForm())
const localErrors = reactive<Record<string, string>>({})
const revenueUnitOf = (company: Company): RevenueUnit =>
  company.revenueUnit ?? (company.annualRevenue !== null && company.annualRevenue >= 100_000_000 ? 'HUNDRED_MILLION' : 'TEN_THOUSAND')
const revenueDivisor = (unit: RevenueUnit) => unit === 'HUNDRED_MILLION' ? 100_000_000 : 10_000

watch(
  () => props.company,
  (company) => {
    Object.assign(form, company
      ? {
          name: company.name,
          websiteUrl: company.websiteUrl ?? '',
          industries: [...company.industries],
          companySize: company.companySize,
          revenueUnit: revenueUnitOf(company),
          annualRevenueText: company.annualRevenue === null ? '' : String(company.annualRevenue / revenueDivisor(revenueUnitOf(company))),
          employeeCountText: company.employeeCount === null ? '' : String(company.employeeCount),
          address: company.address ?? '',
          // input[type=month] 는 YYYY-MM 만 받는다. 저장은 그 달 1일로 한다.
          foundedOn: company.foundedOn ? company.foundedOn.slice(0, 7) : '',
          summary: company.summary ?? '',
          benefits: company.benefits ?? '',
          memo: company.memo ?? '',
        }
      : emptyForm())
    Object.keys(localErrors).forEach((key) => delete localErrors[key])
  },
  { immediate: true },
)

const fieldError = (name: string) => localErrors[name] ?? props.apiFieldErrors[name]
const firstError = computed(() => Object.values(localErrors)[0] ?? Object.values(props.apiFieldErrors)[0] ?? '')

const parseNumber = (value: string | number) => (String(value).trim() === '' ? null : Number(value))

function validate() {
  Object.keys(localErrors).forEach((key) => delete localErrors[key])
  if (!form.name.trim()) localErrors.name = '기업명을 입력해 주세요.'
  else if (form.name.length > 120) localErrors.name = '기업명은 120자 이하여야 합니다.'
  if (form.websiteUrl && !/^https?:\/\/.+/i.test(form.websiteUrl)) {
    localErrors.websiteUrl = 'http 또는 https URL을 입력해 주세요.'
  }
  const revenue = parseNumber(form.annualRevenueText)
  if (revenue !== null) {
    const revenueWon = Math.round(revenue * revenueDivisor(form.revenueUnit))
    if (!Number.isFinite(revenueWon) || revenueWon < 0 || !Number.isSafeInteger(revenueWon)) {
      localErrors.annualRevenue = '0 이상 숫자를 입력해 주세요.'
    }
  }
  const employees = parseNumber(form.employeeCountText)
  if (employees !== null && (!Number.isInteger(employees) || employees < 0)) {
    localErrors.employeeCount = '0 이상 정수를 입력해 주세요.'
  }
  return Object.keys(localErrors).length === 0
}

function submit() {
  if (!validate()) return
  const revenue = parseNumber(form.annualRevenueText)
  const annualRevenue = revenue === null ? null : Math.round(revenue * revenueDivisor(form.revenueUnit))
  emit('submit', {
    name: form.name.trim(),
    websiteUrl: form.websiteUrl.trim(),
    industries: form.industries,
    companySize: form.companySize,
    annualRevenue,
    revenueUnit: annualRevenue === null ? null : form.revenueUnit,
    employeeCount: parseNumber(form.employeeCountText),
    address: form.address.trim(),
    foundedOn: form.foundedOn ? `${form.foundedOn}-01` : null,
    summary: form.summary,
    benefits: form.benefits,
    memo: form.memo,
  })
}

const sizes = Object.entries(companySizeLabels) as Array<[CompanySize, string]>
const revenueUnits: Array<[RevenueUnit, string]> = [['TEN_THOUSAND', '만 원'], ['HUNDRED_MILLION', '억 원']]
</script>

<template>
  <!-- 껍데기는 Drawer 가 가진다. 저장 버튼도 드로어 하단에서 form="company-form" 으로 제출한다. -->
  <form id="company-form" @submit.prevent="submit">
    <div v-if="firstError" class="notice error form-error-summary" role="alert">{{ firstError }}</div>
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
        <textarea v-model="form.summary" maxlength="2000" rows="3" placeholder="기업과 직무를 빠르게 파악할 수 있는 내용" />
      </label>

      <p class="form-section">기업 정보</p>
      <div class="field full">
        <span>업종 <small>기존 태그 선택 또는 Enter로 추가</small></span>
        <TagInput v-model="form.industries" :suggestions="industryOptions" placeholder="예: IT서비스" :max="10" />
        <small v-if="fieldError('industries')" class="field-error">{{ fieldError('industries') }}</small>
      </div>
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
        <span class="field-label-with-tools">매출액
          <span class="unit-toggle" role="group" aria-label="매출액 단위">
            <button v-for="[value, label] in revenueUnits" :key="value" type="button" :class="{ active: form.revenueUnit === value }" @click="form.revenueUnit = value">{{ label }}</button>
          </span>
        </span>
        <input v-model="form.annualRevenueText" type="number" min="0" :step="form.revenueUnit === 'HUNDRED_MILLION' ? 0.1 : 1" :placeholder="form.revenueUnit === 'HUNDRED_MILLION' ? '예: 1.2' : '예: 12000'" />
        <small v-if="fieldError('annualRevenue')" class="field-error">{{ fieldError('annualRevenue') }}</small>
      </label>
      <label class="field">
        <span>사원수</span>
        <input v-model="form.employeeCountText" type="number" min="0" step="1" placeholder="420" />
        <small v-if="fieldError('employeeCount')" class="field-error">{{ fieldError('employeeCount') }}</small>
      </label>
      <label class="field full">
        <span>주소</span>
        <input v-model="form.address" maxlength="200" placeholder="예: 경기 성남시 분당구 …" />
        <small v-if="fieldError('address')" class="field-error">{{ fieldError('address') }}</small>
      </label>

      <label class="field full">
        <span>기업 복지</span>
        <textarea v-model="form.benefits" maxlength="5000" rows="4" placeholder="복지 제도, 근무 환경, 지원 항목" />
        <small v-if="fieldError('benefits')" class="field-error">{{ fieldError('benefits') }}</small>
      </label>

      <p class="form-section">개인 기록</p>
      <label class="field full">
        <span>지원 메모</span>
        <textarea v-model="form.memo" maxlength="5000" rows="4" placeholder="연결할 경험, 확인할 질문, 다음 행동" />
      </label>
    </div>
    <p class="form-hint">채용정보는 채용공고를 등록하면 자동으로 채워집니다.</p>
  </form>
</template>
