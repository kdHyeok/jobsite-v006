<script setup lang="ts">
import { computed, onUnmounted, ref, watch } from 'vue'
import ResumeSection from './ResumeSection.vue'
import type { Resume, ResumeContent, ResumePayload, Row, SectionKey } from '../types/resume'
import { BASIC_FIELDS, MAX_LENGTH, SECTIONS, fillContent } from '../types/resume'

/**
 * 이력서 한 버전의 전체 페이지 편집기. 폼이 커서 드로어(520px)에 넣지 않는다 — design-system 규칙 7.
 * 상태는 여기(draft)에 두고 저장 버튼에서 한 번에 PUT 한다.
 */
const props = defineProps<{
  resume: Resume
  saving: boolean
  apiFieldErrors: Record<string, string>
}>()

const emit = defineEmits<{
  save: [payload: ResumePayload]
  copy: []
  remove: []
  back: [dirty: boolean]
}>()

const name = ref(props.resume.name)
const content = ref<ResumeContent>(fillContent(props.resume.content))

watch(() => props.resume, (resume) => {
  name.value = resume.name
  content.value = fillContent(resume.content)
})

/** 저장한 시점의 문서와 깊은 비교. 입력마다 새 객체를 만드니 JSON 비교가 정직하다. */
const dirty = computed(() =>
  name.value !== props.resume.name
  || JSON.stringify(content.value) !== JSON.stringify(fillContent(props.resume.content)),
)

// 저장 안 한 변경이 있으면 탭 닫기·새로고침에 경고. 한 줄이면 되는 일을 라이브러리로 하지 않는다.
const guard = (event: BeforeUnloadEvent) => { event.preventDefault() }
watch(dirty, (isDirty) => {
  if (isDirty) window.addEventListener('beforeunload', guard)
  else window.removeEventListener('beforeunload', guard)
}, { immediate: true })
onUnmounted(() => window.removeEventListener('beforeunload', guard))

function setBasic(key: string, value: string) {
  content.value = { ...content.value, basic: { ...content.value.basic, [key]: value } }
}
function setRows(key: SectionKey, rows: Row[]) {
  content.value = { ...content.value, [key]: rows }
}

function submit() {
  if (!name.value.trim()) return
  emit('save', { name: name.value.trim(), content: content.value })
}

const basicValue = (key: string) => content.value.basic[key as keyof typeof content.value.basic] ?? ''
</script>

<template>
  <main class="page">
    <div class="page-head resume-head">
      <button type="button" class="button ghost compact" @click="emit('back', dirty)">← 목록</button>
      <input
        v-model="name"
        class="resume-name"
        maxlength="120"
        aria-label="이력서 이름"
        placeholder="이력서 이름 (예: 백엔드 신입 v2)"
      />
      <span v-if="dirty" class="resume-dirty">변경됨 · 저장 전</span>
      <div class="page-tools">
        <button type="button" class="button secondary compact" :disabled="saving" @click="emit('copy')">복제</button>
        <button type="button" class="button danger compact" :disabled="saving" @click="emit('remove')">삭제</button>
        <button type="button" class="button primary" :disabled="saving || !dirty || !name.trim()" @click="submit">
          {{ saving ? '저장 중…' : '저장' }}
        </button>
      </div>
    </div>
    <p v-if="!name.trim()" class="field-error">이력서 이름을 입력해 주세요.</p>
    <p v-else-if="apiFieldErrors.name" class="field-error">{{ apiFieldErrors.name }}</p>

    <section class="resume-section">
      <div class="resume-section__head"><h2>기본정보</h2></div>
      <div class="form-grid">
        <label v-for="field in BASIC_FIELDS" :key="field.key" class="field" :class="{ full: field.kind === 'url' }">
          <span>{{ field.label }}</span>
          <input
            :value="basicValue(field.key)"
            :maxlength="MAX_LENGTH[field.kind]"
            :placeholder="field.placeholder"
            @input="setBasic(field.key, ($event.target as HTMLInputElement).value)"
          />
        </label>
      </div>
    </section>

    <ResumeSection
      v-for="section in SECTIONS"
      :key="section.key"
      :section="section"
      :rows="content[section.key]"
      @update:rows="setRows(section.key, $event)"
    />
  </main>
</template>
