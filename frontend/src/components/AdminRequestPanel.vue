<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { ApiClientError } from '../api/http'
import { deleteAdminFeedback, deleteAdminRequest, listAdminRequests, saveAdminFeedback } from '../api/admin-requests'
import { adminRequestKindLabels } from '../types/admin-request'
import type { AdminRequestForAdmin } from '../types/admin-request'
import ConfirmDialog from './ConfirmDialog.vue'

const items = ref<AdminRequestForAdmin[]>([])
const loading = ref(true)
const error = ref('')
const notice = ref('')
const filter = ref<'OPEN' | 'ANSWERED' | 'ALL'>('OPEN')
const editingId = ref<string | null>(null)
const feedbackDraft = ref('')
const busyId = ref<string | null>(null)
const deleteTarget = ref<{ item: AdminRequestForAdmin; kind: 'request' | 'feedback' } | null>(null)
const visible = computed(() => items.value.filter((item) =>
  filter.value === 'ALL' || (filter.value === 'OPEN' ? !item.feedback : !!item.feedback)))
const openCount = computed(() => items.value.filter((item) => !item.feedback).length)

function report(cause: unknown, fallback: string) {
  error.value = cause instanceof ApiClientError ? cause.message : fallback
}
async function load() {
  loading.value = true; error.value = ''
  try { items.value = await listAdminRequests() }
  catch (cause) { report(cause, '관리자 요청을 불러오지 못했습니다.') }
  finally { loading.value = false }
}
function startFeedback(item: AdminRequestForAdmin) {
  editingId.value = item.id
  feedbackDraft.value = item.feedback ?? ''
  error.value = ''; notice.value = ''
}
function replace(updated: AdminRequestForAdmin) {
  items.value = items.value.map((item) => item.id === updated.id ? updated : item)
}
async function submitFeedback(item: AdminRequestForAdmin) {
  busyId.value = item.id; error.value = ''; notice.value = ''
  try {
    replace(await saveAdminFeedback(item.id, feedbackDraft.value))
    editingId.value = null
    notice.value = '피드백을 저장했습니다.'
  } catch (cause) { report(cause, '피드백을 저장하지 못했습니다.') }
  finally { busyId.value = null }
}
async function remove() {
  if (!deleteTarget.value) return
  const { item, kind } = deleteTarget.value
  busyId.value = item.id; error.value = ''; notice.value = ''
  try {
    if (kind === 'feedback') {
      replace(await deleteAdminFeedback(item.id))
      notice.value = '피드백을 삭제했습니다.'
    } else {
      await deleteAdminRequest(item.id)
      items.value = items.value.filter((current) => current.id !== item.id)
      notice.value = '요청을 삭제했습니다.'
    }
    deleteTarget.value = null
  } catch (cause) { report(cause, '삭제하지 못했습니다.'); deleteTarget.value = null }
  finally { busyId.value = null }
}
function formatDate(value: string) {
  return new Date(value).toLocaleString('ko-KR', { dateStyle: 'medium', timeStyle: 'short' })
}
onMounted(load)
</script>

<template>
  <div v-if="notice" class="notice success" role="status">{{ notice }}</div>
  <div v-if="error" class="notice error" role="alert"><span>{{ error }}</span><button type="button" @click="load">다시 시도</button></div>
  <div class="tabs" role="tablist" aria-label="관리자 요청 필터">
    <button type="button" role="tab" :aria-selected="filter === 'OPEN'" :class="{ active: filter === 'OPEN' }" @click="filter = 'OPEN'">미답변 <span class="tab-count">{{ openCount }}</span></button>
    <button type="button" role="tab" :aria-selected="filter === 'ANSWERED'" :class="{ active: filter === 'ANSWERED' }" @click="filter = 'ANSWERED'">피드백 완료</button>
    <button type="button" role="tab" :aria-selected="filter === 'ALL'" :class="{ active: filter === 'ALL' }" @click="filter = 'ALL'">전체</button>
  </div>
  <section v-if="loading" class="loading-card" aria-live="polite"><span class="spinner" /> 요청을 불러오는 중입니다.</section>
  <section v-else class="admin-request-list">
    <article v-for="item in visible" :key="item.id" class="admin-request-card">
      <header>
        <div>
          <span class="chip" :data-tone="item.kind === 'BUG_REPORT' ? 'negative' : 'primary'">{{ adminRequestKindLabels[item.kind] }}</span>
          <strong>{{ item.requesterName || item.requesterEmail }}</strong>
          <span class="muted-note">{{ item.requesterEmail }}</span>
        </div>
        <time>{{ formatDate(item.createdAt) }}</time>
      </header>
      <p class="body-copy">{{ item.message }}</p>
      <form v-if="editingId === item.id" class="request-form" @submit.prevent="submitFeedback(item)">
        <label class="field"><span>관리자 피드백</span><textarea v-model="feedbackDraft" rows="5" minlength="5" maxlength="2000" required :disabled="busyId === item.id" /></label>
        <div class="request-actions">
          <button type="button" class="button secondary compact" @click="editingId = null">취소</button>
          <button type="submit" class="button primary compact" :disabled="busyId === item.id">저장</button>
        </div>
      </form>
      <div v-else-if="item.feedback" class="request-feedback">
        <strong>피드백</strong>
        <p class="body-copy">{{ item.feedback }}</p>
        <span class="muted-note">{{ item.feedbackUnread ? '사용자 미확인' : '사용자 확인' }}</span>
      </div>
      <footer class="request-actions">
        <button type="button" class="button secondary compact" @click="startFeedback(item)">{{ item.feedback ? '피드백 수정' : '피드백 작성' }}</button>
        <button v-if="item.feedback" type="button" class="button danger compact" @click="deleteTarget = { item, kind: 'feedback' }">피드백 삭제</button>
        <button type="button" class="button danger compact" @click="deleteTarget = { item, kind: 'request' }">요청 삭제</button>
      </footer>
    </article>
    <div v-if="visible.length === 0" class="empty-state"><strong>표시할 요청이 없습니다.</strong></div>
  </section>
  <ConfirmDialog v-if="deleteTarget" :title="deleteTarget.kind === 'feedback' ? '피드백을 삭제할까요?' : '요청을 삭제할까요?'"
    :subject="adminRequestKindLabels[deleteTarget.item.kind]" :detail="deleteTarget.kind === 'feedback' ? ' 피드백만 삭제됩니다.' : ' 요청과 피드백이 모두 삭제됩니다.'"
    :aria-label="deleteTarget.kind === 'feedback' ? '피드백 삭제 확인' : '요청 삭제 확인'" :busy="busyId === deleteTarget.item.id"
    @confirm="remove" @cancel="deleteTarget = null" />
</template>
