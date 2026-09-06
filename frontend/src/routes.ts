/**
 * 프런트엔드가 아는 모든 경로. 이 파일 밖에서 경로 문자열 리터럴을 쓰지 않는다.
 *
 * base URL 은 여기 없다. nginx 뒤에서 same-origin 으로 동작하므로 항상 상대 경로를 쓴다.
 * 절대 URL 을 조립하면 프록시 포트 차이(80 vs 8088 vs 8443)에 그대로 노출된다.
 *
 * 백엔드 대응 상수: backend/src/main/java/com/jobsight/company/common/ApiPaths.java
 * nginx 프록시 접두사: frontend/nginx.conf 상단 표
 */

/** SPA 라우트. App.vue 가 pathname 으로 분기한다. 홈은 채용공고. */
export const ROUTES = {
  home: '/',
  positions: '/positions',
  companies: '/companies',
  admin: '/admin',
  plugin: '/plugin',
} as const

/** 백엔드 API. */
export const API = {
  me: '/api/auth/me',
  loginOptions: '/api/auth/login-options',
  logout: '/api/auth/logout',
  companies: '/api/companies',
  company: (id: string) => `/api/companies/${id}`,
  companyContents: (companyId: string) => `/api/companies/${companyId}/contents`,
  companyContent: (companyId: string, id: string) => `/api/companies/${companyId}/contents/${id}`,
  postings: '/api/postings',
  posting: (id: string) => `/api/postings/${id}`,
  postingStatus: (id: string) => `/api/postings/${id}/status`,
  postingStep: (id: string, seq: number) => `/api/postings/${id}/steps/${seq}`,
  postingArchive: (id: string) => `/api/postings/${id}/archive`,
  positions: '/api/positions',
  position: (id: string) => `/api/positions/${id}`,
  positionReferences: (id: string) => `/api/positions/${id}/references`,
  references: '/api/references',
  reference: (id: string) => `/api/references/${id}`,
  adminUsers: '/api/admin/users',
  adminUser: (id: string) => `/api/admin/users/${id}`,
  adminUserStatus: (id: string) => `/api/admin/users/${id}/status`,
  adminUserRole: (id: string) => `/api/admin/users/${id}/role`,
  adminUserName: (id: string) => `/api/admin/users/${id}/name`,
  adminSettings: '/api/admin/settings',
} as const

/** Spring Security 가 소유하는 경로. 브라우저 네비게이션으로만 쓴다(fetch 금지). */
export const GOOGLE_LOGIN_URL = '/oauth2/authorization/google'

/** MCP 클라이언트용 경로. SPA 쿠키 API와 별개인 OAuth 위임 채널. */
export const MCP_ENDPOINTS = {
  config: '/plugin/config',
  skill: '/plugin/skill',
  download: '/plugin/download',
  server: '/mcp',
  resourceMetadata: '/.well-known/oauth-protected-resource',
  authorizationMetadata: '/.well-known/oauth-authorization-server',
  authorize: '/oauth2/authorize',
  token: '/oauth2/token',
  revoke: '/oauth2/revoke',
  introspect: '/oauth2/introspect',
} as const

export const PLUGIN_LINKS = {
  docs: 'https://developers.openai.com/plugins/deploy/connect-chatgpt',
} as const

/**
 * 다른 화면의 항목을 열어 달라고 넘기는 쿼리 파라미터.
 * 드로어 안의 종속 데이터를 더블클릭하면 `/positions?focus=<id>` 처럼 이동한다.
 * 링크로 공유·북마크할 수 있게 URL 에 남긴다.
 */
export const FOCUS_QUERY = 'focus'

/** Google 로그인 실패 시 백엔드가 붙여 돌려보내는 쿼리 파라미터 이름. */
export const AUTH_ERROR_QUERY = 'authError'
