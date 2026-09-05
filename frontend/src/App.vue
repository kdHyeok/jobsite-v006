<script setup lang="ts">
import { computed, onMounted, onUnmounted, ref } from 'vue'
import { fetchMe, logout } from './api/auth'
import AdminView from './components/AdminView.vue'
import CompanyWorkspace from './components/CompanyWorkspace.vue'
import LoginView from './components/LoginView.vue'
import PostingBoard from './components/PostingBoard.vue'
import UserMenu from './components/UserMenu.vue'
import { ROUTES } from './routes'
import type { Me } from './types/auth'

const ANONYMOUS: Me = { authenticated: false, id: null, email: null, displayName: null, role: null }

// 라우트가 몇 개(ROUTES)뿐이라 라우터 라이브러리를 두지 않았다.
// nginx가 모든 경로에 index.html을 돌려주므로 pathname만 보면 된다.
const path = ref(window.location.pathname)
const me = ref<Me>(ANONYMOUS)
const booting = ref(true)
const bootError = ref('')

const isAdmin = computed(() => me.value.role === 'ADMIN')
const onAdminRoute = computed(() => path.value.startsWith(ROUTES.admin))
const onPostingsRoute = computed(() => path.value.startsWith(ROUTES.postings))

function syncPath() {
  path.value = window.location.pathname
}

function navigate(next: string) {
  if (window.location.pathname !== next) {
    window.history.pushState({}, '', next)
  }
  path.value = next
}

async function loadMe() {
  booting.value = true
  bootError.value = ''
  try {
    me.value = await fetchMe()
  } catch (error) {
    bootError.value = error instanceof Error ? error.message : '세션 상태를 확인하지 못했습니다.'
    me.value = ANONYMOUS
  } finally {
    booting.value = false
  }
}

async function signOut() {
  try {
    await logout()
  } finally {
    me.value = ANONYMOUS
    navigate(ROUTES.home)
  }
}

onMounted(() => {
  window.addEventListener('popstate', syncPath)
  loadMe()
})

onUnmounted(() => {
  window.removeEventListener('popstate', syncPath)
})
</script>

<template>
  <div class="app-shell">
    <header class="topbar">
      <button type="button" class="brand" @click="navigate(ROUTES.home)">
        <span class="brand-mark">J</span>
        JobSight
      </button>

      <!-- 세그먼트에는 성격이 같은 두 화면만 둔다. 관리자는 오른쪽에 따로. -->
      <nav v-if="me.authenticated" class="segmented" aria-label="화면 전환">
        <button
          type="button"
          :class="{ active: !onAdminRoute && !onPostingsRoute }"
          @click="navigate(ROUTES.home)"
        >
          기업
        </button>
        <button type="button" :class="{ active: onPostingsRoute }" @click="navigate(ROUTES.postings)">
          채용공고
        </button>
      </nav>
      <span v-else />

      <div class="topbar-actions">
        <template v-if="me.authenticated">
          <button
            v-if="isAdmin"
            type="button"
            class="button secondary compact"
            @click="navigate(onAdminRoute ? ROUTES.home : ROUTES.admin)"
          >
            {{ onAdminRoute ? '워크스페이스' : '관리자' }}
          </button>
          <UserMenu :me="me" @updated="me = $event" @signed-out="signOut" />
        </template>
      </div>
    </header>

    <main v-if="booting" class="page">
      <div class="loading-card" aria-live="polite">
        <span class="spinner" /> 세션을 확인하는 중입니다.
      </div>
    </main>

    <template v-else-if="!me.authenticated">
      <main v-if="bootError" class="page">
        <div class="notice error" role="alert">
          <span>{{ bootError }}</span>
          <button type="button" @click="loadMe">다시 시도</button>
        </div>
      </main>
      <LoginView />
    </template>

    <template v-else-if="onAdminRoute">
      <AdminView v-if="isAdmin" :me="me" />
      <main v-else class="page">
        <div class="empty-state">
          <strong>관리자 권한이 필요합니다.</strong>
          <p>이 페이지는 관리자 계정만 열 수 있습니다.</p>
          <button type="button" class="button primary" @click="navigate(ROUTES.home)">
            워크스페이스로 이동
          </button>
        </div>
      </main>
    </template>

    <PostingBoard v-else-if="onPostingsRoute" />

    <CompanyWorkspace v-else />
  </div>
</template>
