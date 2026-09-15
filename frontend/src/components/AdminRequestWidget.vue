<script setup lang="ts">
import { computed, onMounted, onUnmounted, ref } from 'vue'
import { ApiClientError } from '../api/http'
import {
  createAdminRequest, deleteMyAdminRequest, listMyAdminRequests, readAdminFeedback, updateAdminRequest,
} from '../api/admin-requests'
import { adminRequestKindLabels } from '../types/admin-request'
import type { AdminRequest, AdminRequestInput } from '../types/admin-request'
import ConfirmDialog from './ConfirmDialog.vue'

const open = ref(false)
const writing = ref(false)
const loading = ref(false)
const saving = ref(false)
const error = ref('')
const notice = ref('')
const items = ref<AdminRequest[]>([])
const editingId = ref<string | null>(null)
const expandedFeedbackId = ref<string | null>(null)
const deleteTarget = ref<AdminRequest | null>(null)
const draft = ref<AdminRequestInput>({ kind: 'BUG_REPORT', message: '' })
const unreadCount = computed(() => items.value.filter((item) => item.feedbackUnread).length)

function report(cause: unknown, fallback: string) {
  error.value = cause instanceof ApiClientError ? cause.message : fallback
}
async function load() {
  loading.value = true; error.value = ''
  try { items.value = await listMyAdminRequests() }
  catch (cause) { report(cause, '요청 목록을 불러오지 못했습니다.') }
  finally { loading.value = false }
}
async function toggle() {
  open.value = !open.value
  if (open.value) await load()
}
function startNew() {
  editingId.value = null
  draft.value = { kind: 'BUG_REPORT', message: '' }
  writing.value = true; error.value = ''; notice.value = ''
}
function startEdit(item: AdminRequest) {
  editingId.value = item.id
  draft.value = { kind: item.kind, message: item.message }
  writing.value = true; error.value = ''; notice.value = ''
}
async function save() {
  saving.value = true; error.value = ''; notice.value = ''
  try {
    const saved = editingId.value
      ? await updateAdminRequest(editingId.value, draft.value)
      : await createAdminRequest(draft.value)
    items.value = editingId.value
      ? items.value.map((item) => item.id === saved.id ? saved : item)
      : [saved, ...items.value]
    writing.value = false
    notice.value = editingId.value ? '요청을 수정했습니다.' : '관리자에게 요청을 보냈습니다.'
  } catch (cause) { report(cause, '요청을 저장하지 못했습니다.') }
  finally { saving.value = false }
}
async function remove() {
  if (!deleteTarget.value) return
  const id = deleteTarget.value.id
  saving.value = true; error.value = ''
  try {
    await deleteMyAdminRequest(id)
    items.value = items.value.filter((item) => item.id !== id)
    deleteTarget.value = null
    notice.value = '요청을 삭제했습니다.'
  } catch (cause) { report(cause, '요청을 삭제하지 못했습니다.'); deleteTarget.value = null }
  finally { saving.value = false }
}
async function toggleFeedback(item: AdminRequest) {
  expandedFeedbackId.value = expandedFeedbackId.value === item.id ? null : item.id
  if (expandedFeedbackId.value && item.feedbackUnread) {
    try {
      const updated = await readAdminFeedback(item.id)
      items.value = items.value.map((current) => current.id === updated.id ? updated : current)
    } catch (cause) { report(cause, '피드백 확인 상태를 저장하지 못했습니다.') }
  }
}
function onKeydown(event: KeyboardEvent) { if (event.key === 'Escape') open.value = false }
onMounted(() => window.addEventListener('keydown', onKeydown))
onUnmounted(() => window.removeEventListener('keydown', onKeydown))
function formatDate(value: string) {
  return new Date(value).toLocaleString('ko-KR', { dateStyle: 'short', timeStyle: 'short' })
}
</script>

<template>
  <button type="button" class="request-fab" :aria-expanded="open" aria-controls="admin-request-widget" @click="toggle">
    관리자 요청 <span v-if="unreadCount" class="request-fab__badge">{{ unreadCount }}</span>
  </button>
  <section v-if="open" id="admin-request-widget" class="request-widget" role="dialog" aria-label="관리자에게 요청하기">
    <header class="request-widget__head">
      <div><strong>관리자에게 요청하기</strong><span>최근 요청 {{ items.length }}건</span></div>
      <button type="button" class="icon-button" aria-label="요청 창 닫기" @click="open = false">×</button>
    </header>
    <div class="request-widget__body">
      <div v-if="notice" class="notice success" role="status">{{ notice }}</div>
      <div v-if="error" class="notice error" role="alert">{{ error }}</div>

      <form v-if="writing" class="request-form" @submit.prevent="save">
        <label class="field"><span>요청 종류</span>
          <select v-model="draft.kind" :disabled="saving">
            <option v-for="(label, value) in adminRequestKindLabels" :key="value" :value="value">{{ label }}</option>
          </select>
        </label>
        <label class="field"><span>메시지</span>
          <textarea v-model="draft.message" rows="7" minlength="5" maxlength="2000" required :disabled="saving" placeholder="관리자에게 전달할 내용을 입력하세요." />
        </label>
        <div class="request-actions">
          <button type="button" class="button secondary" :disabled="saving" @click="writing = false">취소</button>
          <button type="submit" class="button primary" :disabled="saving">{{ saving ? '저장 중…' : editingId ? '수정' : '보내기' }}</button>
        </div>
      </form>

      <template v-else>
        <button type="button" class="button primary request-new" @click="startNew">신규 요청</button>
        <p v-if="loading" class="muted-note" role="status">요청 목록을 불러오는 중입니다.</p>
        <div v-else class="request-list">
          <article v-for="item in items" :key="item.id" class="request-card">
            <div class="request-card__meta">
              <span class="chip" :data-tone="item.kind === 'BUG_REPORT' ? 'negative' : 'primary'">{{ adminRequestKindLabels[item.kind] }}</span>
              <time>{{ formatDate(item.updatedAt) }}</time>
            </div>
            <p>{{ item.message }}</p>
            <button v-if="item.feedback" type="button" class="feedback-button" @click="toggleFeedback(item)">
              관리자 피드백 <span v-if="item.feedbackUnread" class="chip" data-tone="warning">새 피드백</span>
            </button>
            <div v-if="item.feedback && expandedFeedbackId === item.id" class="request-feedback">{{ item.feedback }}</div>
            <div class="request-actions">
              <button type="button" class="button ghost compact" @click="startEdit(item)">수정</button>
              <button type="button" class="button danger compact" @click="deleteTarget = item">삭제</button>
            </div>
          </article>
          <p v-if="items.length === 0" class="muted-note">아직 보낸 요청이 없습니다.</p>
        </div>
      </template>
    </div>
  </section>
  <ConfirmDialog v-if="deleteTarget" title="요청을 삭제할까요?" :subject="adminRequestKindLabels[deleteTarget.kind]"
    detail=" 요청과 관리자 피드백이 함께 삭제됩니다." aria-label="관리자 요청 삭제 확인" :busy="saving"
    @confirm="remove" @cancel="deleteTarget = null" />
</template>
