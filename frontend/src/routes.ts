/**
 * 프런트엔드가 아는 모든 경로. 이 파일 밖에서 경로 문자열 리터럴을 쓰지 않는다.
 *
 * base URL 은 여기 없다. nginx 뒤에서 same-origin 으로 동작하므로 항상 상대 경로를 쓴다.
 * 절대 URL 을 조립하면 프록시 포트 차이(80 vs 8088 vs 8443)에 그대로 노출된다.
 *
 * 백엔드 대응 상수: backend/src/main/java/com/jobsight/company/common/ApiPaths.java
 * nginx 프록시 접두사: frontend/nginx.conf 상단 표
 */

/** SPA 라우트. App.vue 가 pathname 으로 분기한다. */
export const ROUTES = {
  home: '/',
  admin: '/admin',
} as const

/** 백엔드 API. */
export const API = {
  me: '/api/auth/me',
  loginOptions: '/api/auth/login-options',
  logout: '/api/auth/logout',
  companies: '/api/companies',
  company: (id: string) => `/api/companies/${id}`,
  adminUsers: '/api/admin/users',
  adminUser: (id: string) => `/api/admin/users/${id}`,
  adminUserStatus: (id: string) => `/api/admin/users/${id}/status`,
  adminUserRole: (id: string) => `/api/admin/users/${id}/role`,
  adminUserName: (id: string) => `/api/admin/users/${id}/name`,
  adminSettings: '/api/admin/settings',
} as const

/** Spring Security 가 소유하는 경로. 브라우저 네비게이션으로만 쓴다(fetch 금지). */
export const GOOGLE_LOGIN_URL = '/oauth2/authorization/google'

/** Google 로그인 실패 시 백엔드가 붙여 돌려보내는 쿼리 파라미터 이름. */
export const AUTH_ERROR_QUERY = 'authError'
