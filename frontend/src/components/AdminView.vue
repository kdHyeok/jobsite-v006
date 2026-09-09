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
  listUserDataCounts,
  updateAutoApproveSignup,
} from '../api/admin'
import ConfirmDialog from './ConfirmDialog.vue'
import { roleLabels, userStatusLabels } from '../types/auth'
import type { AdminUser, Me, UserDataCount, UserRole, UserStatus } from '../types/auth'

const props = defineProps<{ me: Me }>()

const users = ref<AdminUser[]>([])
/**
 * 계정별 등록 수. 계정 목록과 따로 들고 있다 — 상태·권한을 바꾸면 그 행을 새 UserResponse 로
 * 갈아끼우는데, 같은 객체에 넣었으면 그때마다 개수가 사라진다.
 */
const dataCounts = ref(new Map<string, UserDataCount>())
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

/** 상태 칩 색. PENDING 은 주의, ACTIVE 는 긍정. */
const statusTone: Record<UserStatus, string> = {
  PENDING: 'warning', ACTIVE: 'positive', SUSPENDED: 'negative', REJECTED: 'neutral',
}

const pendingCount = computed(() => users.value.filter((user) => user.status === 'PENDING').length)

const EMPTY_COUNT = { companies: 0, postings: 0, positions: 0 }
/** 응답에 없는 계정은 등록한 데이터가 하나도 없는 계정이다. */
const countsOf = (id: string) => dataCounts.value.get(id) ?? EMPTY_COUNT

function report(error: unknown, fallback: string) {
  errorMessage.value = error instanceof ApiClientError ? error.message : fallback
}

async function load() {
  loading.value = true
  errorMessage.value = ''
  try {
    // 개수는 곁다리다. 못 읽어도 계정 관리는 되어야 하므로 여기서 삼킨다.
    const [list, settings, counts] = await Promise.all([
      listUsers(),
      fetchSettings(),
      listUserDataCounts().catch(() => [] as UserDataCount[]),
    ])
    users.value = list
    autoApproveSignup.value = settings.autoApproveSignup
    dataCounts.value = new Map(counts.map((count) => [count.userId, count]))
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
  <main class="page">
    <div class="page-head">
      <div class="page-title">
        <h1>관리자</h1>
        <span class="page-count">승인 대기 {{ pendingCount }}</span>
      </div>
    </div>

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

    <section v-else class="table-wrap">
      <table class="data-table">
        <thead>
          <tr>
            <th>이메일</th>
            <th>이름</th>
            <th>로그인 수단</th>
            <th>권한</th>
            <th>상태</th>
            <th class="data-table__num">기업</th>
            <th class="data-table__num">공고</th>
            <th class="data-table__num">직무</th>
            <th>신청일</th>
            <th class="data-table__actions">처리</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="user in users" :key="user.id">
            <td>
              <strong>{{ user.email }}</strong>
              <span v-if="user.id === props.me.id" class="chip">본인</span>
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
              <span class="chip" :data-tone="user.googleLinked ? 'primary' : 'warning'">
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
              <span class="chip" :data-tone="statusTone[user.status]">
                {{ userStatusLabels[user.status] }}
              </span>
            </td>
            <td class="data-table__num" :data-zero="countsOf(user.id).companies === 0">
              {{ countsOf(user.id).companies }}
            </td>
            <td class="data-table__num" :data-zero="countsOf(user.id).postings === 0">
              {{ countsOf(user.id).postings }}
            </td>
            <td class="data-table__num" :data-zero="countsOf(user.id).positions === 0">
              {{ countsOf(user.id).positions }}
            </td>
            <td class="data-table__date">{{ formatDate(user.createdAt) }}</td>
            <td class="data-table__actions">
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
            <td colspan="10" class="data-table__empty">계정이 없습니다.</td>
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
