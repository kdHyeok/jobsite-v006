<script setup lang="ts">
import { reactive, watch } from 'vue'
import type { Position, PositionPayload } from '../types/position'

const props = defineProps<{
  position: Position
  saving: boolean
  apiFieldErrors: Record<string, string>
}>()

const emit = defineEmits<{ submit: [payload: PositionPayload] }>()

type FormState = Omit<PositionPayload, 'techStack'> & { techStackText: string }

const form = reactive<FormState>({
  name: '', team: '', role: '', responsibilities: '', impact: '', growth: '', experience: '',
  requiredSkills: '', preferredSkills: '', headcount: '', workLocation: '', techStackText: '',
})
const localErrors = reactive<Record<string, string>>({})

watch(
  () => props.position,
  (position) => {
    Object.assign(form, {
      name: position.name,
      team: position.team ?? '',
      role: position.role ?? '',
      responsibilities: position.responsibilities ?? '',
      impact: position.impact ?? '',
      growth: position.growth ?? '',
      experience: position.experience ?? '',
      requiredSkills: position.requiredSkills ?? '',
      preferredSkills: position.preferredSkills ?? '',
      headcount: position.headcount ?? '',
      workLocation: position.workLocation ?? '',
      techStackText: position.techStack.join(', '),
    })
    Object.keys(localErrors).forEach((key) => delete localErrors[key])
  },
  { immediate: true },
)

const fieldError = (name: string) => localErrors[name] ?? props.apiFieldErrors[name]

function submit() {
  Object.keys(localErrors).forEach((key) => delete localErrors[key])
  if (!form.name.trim()) {
    localErrors.name = '직무 이름을 입력해 주세요.'
    return
  }
  const { techStackText, ...rest } = form
  emit('submit', {
    ...rest,
    name: form.name.trim(),
    // 업종과 같은 규칙: 쉼표 분리, 공백·중복 제거, 순서 유지
    techStack: [...new Set(techStackText.split(',').map((v) => v.trim()).filter(Boolean))],
  })
}
</script>

<template>
  <form id="position-form" @submit.prevent="submit">
    <div class="form-grid">
      <label class="field full">
        <span>직무 이름 <b>*</b></span>
        <input v-model="form.name" maxlength="160" autofocus />
        <small v-if="fieldError('name')" class="field-error">{{ fieldError('name') }}</small>
      </label>
      <label class="field">
        <span>소속 팀</span>
        <input v-model="form.team" maxlength="120" placeholder="예: 검색플랫폼팀" />
      </label>
      <label class="field">
        <span>담당 역할</span>
        <input v-model="form.role" maxlength="200" placeholder="예: 백엔드 API 개발" />
      </label>
      <label class="field">
        <span>모집인원</span>
        <input v-model="form.headcount" maxlength="60" placeholder="예: 0명" />
      </label>
      <label class="field">
        <span>근무지역</span>
        <input v-model="form.workLocation" maxlength="160" placeholder="예: 서울 강남구" />
      </label>
      <label class="field full">
        <span>사용 기술 스택 <small>쉼표로 여러 개</small></span>
        <input v-model="form.techStackText" placeholder="예: Java, Spring, Kafka" />
      </label>

      <p class="form-section">일</p>
      <label class="field full">
        <span>담당업무</span>
        <textarea v-model="form.responsibilities" rows="3" />
      </label>
      <label class="field full">
        <span>업무의 영향력</span>
        <textarea v-model="form.impact" rows="2" placeholder="이 일이 어디에 어떤 영향을 주는가" />
      </label>

      <p class="form-section">나</p>
      <label class="field full">
        <span>성장 방향</span>
        <textarea v-model="form.growth" rows="2" placeholder="이 직무로 어떤 부분이 성장하는가" />
      </label>
      <label class="field full">
        <span>취득 경험</span>
        <textarea v-model="form.experience" rows="2" placeholder="이 직무로 얻는 경험" />
      </label>

      <p class="form-section">역량</p>
      <label class="field full">
        <span>요구 역량</span>
        <textarea v-model="form.requiredSkills" rows="3" />
      </label>
      <label class="field full">
        <span>우대 역량</span>
        <textarea v-model="form.preferredSkills" rows="3" />
      </label>
    </div>
  </form>
</template>
