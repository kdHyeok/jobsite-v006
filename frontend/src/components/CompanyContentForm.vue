<script setup lang="ts">
import { reactive, watch } from 'vue'
import type { CompanyContent, CompanyContentPayload } from '../types/company-content'

const props = defineProps<{
  content: CompanyContent | null
  saving: boolean
  apiFieldErrors: Record<string, string>
}>()

const emit = defineEmits<{
  submit: [payload: CompanyContentPayload]
  cancel: []
}>()

const emptyForm = (): CompanyContentPayload => ({
  kind: 'NEWS', title: '', preview: '', source: '', url: '',
})
const form = reactive<CompanyContentPayload>(emptyForm())
const localErrors = reactive<Record<string, string>>({})

watch(
  () => props.content,
  (content) => {
    Object.assign(form, content
      ? { kind: content.kind, title: content.title, preview: content.preview ?? '', source: content.source ?? '', url: content.url }
      : emptyForm())
    Object.keys(localErrors).forEach((key) => delete localErrors[key])
  },
  { immediate: true },
)

const fieldError = (name: string) => localErrors[name] ?? props.apiFieldErrors[name]

function submit() {
  Object.keys(localErrors).forEach((key) => delete localErrors[key])
  if (!form.title.trim()) localErrors.title = '제목을 입력해 주세요.'
  if (!/^https?:\/\/.+/i.test(form.url.trim())) localErrors.url = 'http 또는 https 링크를 입력해 주세요.'
  if (Object.keys(localErrors).length) return
  emit('submit', {
    kind: form.kind,
    title: form.title.trim(),
    preview: form.preview.trim(),
    source: form.source.trim(),
    url: form.url.trim(),
  })
}
</script>

<template>
  <form id="company-content-form" class="content-form form-grid" @submit.prevent="submit">
    <label class="field">
      <span>종류 <b>*</b></span>
      <select v-model="form.kind">
        <option value="NEWS">뉴스</option>
        <option value="YOUTUBE">유튜브</option>
      </select>
    </label>
    <label class="field">
      <span>{{ form.kind === 'NEWS' ? '신문사' : '채널' }}</span>
      <input v-model="form.source" maxlength="160" />
      <small v-if="fieldError('source')" class="field-error">{{ fieldError('source') }}</small>
    </label>
    <label class="field full">
      <span>{{ form.kind === 'NEWS' ? '뉴스 제목' : '유튜브 제목' }} <b>*</b></span>
      <input v-model="form.title" maxlength="200" autofocus />
      <small v-if="fieldError('title')" class="field-error">{{ fieldError('title') }}</small>
    </label>
    <label class="field full">
      <span>{{ form.kind === 'NEWS' ? '본문 미리보기' : '유튜브 설명' }}</span>
      <textarea v-model="form.preview" maxlength="5000" rows="4" />
      <small v-if="fieldError('preview')" class="field-error">{{ fieldError('preview') }}</small>
    </label>
    <label class="field full">
      <span>링크 <b>*</b></span>
      <input v-model="form.url" maxlength="500" placeholder="https://" />
      <small v-if="fieldError('url')" class="field-error">{{ fieldError('url') }}</small>
    </label>
    <div class="content-form__actions full">
      <button type="button" class="button secondary compact" :disabled="saving" @click="emit('cancel')">취소</button>
      <button type="submit" class="button primary compact" :disabled="saving">{{ saving ? '저장 중…' : '저장' }}</button>
    </div>
  </form>
</template>
