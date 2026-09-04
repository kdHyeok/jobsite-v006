<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { ApiClientError } from '../api/http'
import {
  changeUserName,
  changeUserRole,
  changeUserStatus,
  deleteUser,
  fetchSettings,
  listUsers,
  updateAutoApproveSignup,
} from '../api/admin'
import ConfirmDialog from './ConfirmDialog.vue'
import { roleLabels, userStatusLabels } from '../types/auth'
import type { AdminUser, Me, UserRole, UserStatus } from '../types/auth'

const props = defineProps<{ me: Me }>()

const users = ref<AdminUser[]>([])
const autoApproveSignup = ref(false)
const loading = ref(true)
const errorMessage = ref('')
const notice = ref('')
const busyId = ref<string | null>(null)
const savingSettings = ref(false)

// 이름 인라인 편집
const editingId = ref<string | null>(null)
const nameDraft = ref('')

// 삭제 확인
const deleteTarget = ref<AdminUser | null>(null)
const deleting = ref(false)

const pendingCount = computed(() => users.value.filter((user) => user.status === 'PENDING').length)

function report(error: unknown, fallback: string) {
  errorMessage.value = error instanceof ApiClientError ? error.message : fallback
}

async function load() {
  loading.value = true
  errorMessage.value = ''
  try {
    const [list, settings] = await Promise.all([listUsers(), fetchSettings()])
    users.value = list
    autoApproveSignup.value = settings.autoApproveSignup
  } catch (error) {
    report(error, '관리자 정보를 불러오지 못했습니다.')
  } finally {
    loading.value = false
  }
}

function replace(updated: AdminUser) {
  users.value = users.value.map((user) => (user.id === updated.id ? updated : user))
}

async function setStatus(user: AdminUser, status: UserStatus) {
  busyId.value = user.id
  errorMessage.value = ''
  notice.value = ''
  try {
    replace(await changeUserStatus(user.id, status))
    notice.value = `${user.email} 계정을 ${userStatusLabels[status]} 상태로 변경했습니다.`
  } catch (error) {
    report(error, '상태를 변경하지 못했습니다.')
  } finally {
    busyId.value = null
  }
}

async function setRole(user: AdminUser, role: UserRole) {
  busyId.value = user.id
  errorMessage.value = ''
  notice.value = ''
  try {
    replace(await changeUserRole(user.id, role))
    notice.value = `${user.email} 계정의 권한을 ${roleLabels[role]}(으)로 변경했습니다.`
  } catch (error) {
    report(error, '권한을 변경하지 못했습니다.')
    // 서버가 거절했으면 select 표시가 서버 상태와 어긋나므로 목록을 다시 읽는다.
    await load()
  } finally {
    busyId.value = null
  }
}

function startEditName(user: AdminUser) {
  editingId.value = user.id
  nameDraft.value = user.displayName ?? ''
}

function cancelEditName() {
  editingId.value = null
}

async function saveName(user: AdminUser) {
  busyId.value = user.id
  errorMessage.value = ''
  notice.value = ''
  try {
    replace(await changeUserName(user.id, nameDraft.value))
    notice.value = `${user.email} 계정의 이름을 변경했습니다.`
    editingId.value = null
  } catch (error) {
    report(error, '이름을 변경하지 못했습니다.')
  } finally {
    busyId.value = null
  }
}

async function remove() {
  if (!deleteTarget.value) return
  const target = deleteTarget.value
  deleting.value = true
  errorMessage.value = ''
  notice.value = ''
  try {
    await deleteUser(target.id)
    users.value = users.value.filter((user) => user.id !== target.id)
    deleteTarget.value = null
    notice.value = `${target.email} 계정과 소유 기업 정보를 삭제했습니다.`
  } catch (error) {
    report(error, '계정을 삭제하지 못했습니다.')
    deleteTarget.value = null
  } finally {
    deleting.value = false
  }
}

async function toggleAutoApprove(next: boolean) {
  savingSettings.value = true
  errorMessage.value = ''
  notice.value = ''
  try {
    autoApproveSignup.value = (await updateAutoApproveSignup(next)).autoApproveSignup
    notice.value = autoApproveSignup.value
      ? '자동 승인을 켰습니다. 새 계정은 첫 로그인부터 바로 이용할 수 있습니다.'
      : '승인 필요로 바꿨습니다. 새 계정은 승인 대기로 접수됩니다.'
  } catch (error) {
    report(error, '설정을 변경하지 못했습니다.')
  } finally {
    savingSettings.value = false
  }
}

function formatDate(value: string) {
  return new Date(value).toLocaleString('ko-KR', { dateStyle: 'medium', timeStyle: 'short' })
}

onMounted(load)
</script>

<template>
  <main class="workspace">
    <section class="hero">
      <div>
        <p class="eyebrow">ADMIN CONSOLE</p>
        <h1>계정과 권한을 관리하세요.</h1>
        <p>가입 승인, 계정 상태·권한·이름 변경, 계정 삭제, 신규 가입 자동 승인 여부를 이 화면에서 처리합니다.</p>
      </div>
      <div class="metric-card">
        <strong>{{ pendingCount }}</strong>
        <span>승인 대기</span>
      </div>
    </section>

    <div v-if="notice" class="notice success" role="status">{{ notice }}</div>
    <div v-if="errorMessage" class="notice error" role="alert">
      <span>{{ errorMessage }}</span>
      <button type="button" @click="load">다시 시도</button>
    </div>

    <section class="admin-panel">
      <div class="admin-panel__text">
        <p class="label">신규 가입 승인 방식</p>
        <p class="body-copy">
          {{ autoApproveSignup
            ? 'Google 첫 로그인이 곧 가입 완료입니다. 새 계정은 바로 이용할 수 있습니다.'
            : 'Google 첫 로그인은 승인 대기로 접수됩니다. 아래 목록에서 승인해야 이용할 수 있습니다.' }}
        </p>
      </div>
      <button
        type="button"
        class="button"
        :class="autoApproveSignup ? 'secondary' : 'primary'"
        :disabled="savingSettings"
        @click="toggleAutoApprove(!autoApproveSignup)"
      >
        {{ savingSettings ? '변경 중…' : autoApproveSignup ? '승인 필요로 전환' : '자동 승인 켜기' }}
      </button>
    </section>

    <section v-if="loading" class="loading-card" aria-live="polite">
      <span class="spinner" /> 계정 목록을 불러오는 중입니다.
    </section>

    <section v-else class="admin-table-wrap">
      <table class="admin-table">
        <thead>
          <tr>
            <th>이메일</th>
            <th>이름</th>
            <th>로그인 수단</th>
            <th>권한</th>
            <th>상태</th>
            <th>신청일</th>
            <th class="admin-table__actions">처리</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="user in users" :key="user.id">
            <td>
              <strong>{{ user.email }}</strong>
              <span v-if="user.id === props.me.id" class="self-tag">본인</span>
            </td>
            <td>
              <form v-if="editingId === user.id" class="inline-edit" @submit.prevent="saveName(user)">
                <input
                  v-model="nameDraft"
                  maxlength="80"
                  placeholder="비우면 이름 지움"
                  :disabled="busyId === user.id"
                  @keydown.esc.prevent="cancelEditName"
                />
                <button type="submit" class="button primary compact" :disabled="busyId === user.id">저장</button>
                <button type="button" class="button secondary compact" @click="cancelEditName">취소</button>
              </form>
              <button v-else type="button" class="name-button" title="이름 수정" @click="startEditName(user)">
                {{ user.displayName || '—' }}
              </button>
            </td>
            <td>
              <span class="self-tag" :class="{ warn: !user.googleLinked }">
                {{ user.googleLinked ? 'Google' : '없음' }}
              </span>
            </td>
            <td>
              <select
                :value="user.role"
                :disabled="user.id === props.me.id || busyId === user.id"
                @change="setRole(user, ($event.target as HTMLSelectElement).value as UserRole)"
              >
                <option value="USER">{{ roleLabels.USER }}</option>
                <option value="ADMIN">{{ roleLabels.ADMIN }}</option>
              </select>
            </td>
            <td>
              <span class="status-pill" :data-user-status="user.status">
                {{ userStatusLabels[user.status] }}
              </span>
            </td>
            <td class="admin-table__date">{{ formatDate(user.createdAt) }}</td>
            <td class="admin-table__actions">
              <span v-if="user.id === props.me.id" class="muted-note">
                본인 계정은 아바타 메뉴에서 수정합니다
              </span>
              <template v-else>
                <button
                  v-if="user.status !== 'ACTIVE'"
                  type="button"
                  class="button primary compact"
                  :disabled="busyId === user.id"
                  @click="setStatus(user, 'ACTIVE')"
                >
                  {{ user.status === 'PENDING' ? '승인' : '활성화' }}
                </button>
                <button
                  v-if="user.status === 'PENDING'"
                  type="button"
                  class="button danger compact"
                  :disabled="busyId === user.id"
                  @click="setStatus(user, 'REJECTED')"
                >
                  거절
                </button>
                <button
                  v-if="user.status === 'ACTIVE'"
                  type="button"
                  class="button danger compact"
                  :disabled="busyId === user.id"
                  @click="setStatus(user, 'SUSPENDED')"
                >
                  정지
                </button>
                <button
                  type="button"
                  class="button danger compact"
                  :disabled="busyId === user.id"
                  @click="deleteTarget = user"
                >
                  삭제
                </button>
              </template>
            </td>
          </tr>
          <tr v-if="users.length === 0">
            <td colspan="7" class="admin-table__empty">계정이 없습니다.</td>
          </tr>
        </tbody>
      </table>
    </section>
  </main>

  <ConfirmDialog
    v-if="deleteTarget"
    title="계정을 삭제할까요?"
    :subject="deleteTarget.email"
    detail=" 계정과 그 계정이 소유한 기업 정보가 모두 영구 삭제됩니다."
    aria-label="계정 삭제 확인"
    :busy="deleting"
    @confirm="remove"
    @cancel="deleteTarget = null"
  />
</template>
