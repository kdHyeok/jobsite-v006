export type UserRole = 'USER' | 'ADMIN'
export type UserStatus = 'PENDING' | 'ACTIVE' | 'SUSPENDED' | 'REJECTED'

export interface Me {
  authenticated: boolean
  id: string | null
  email: string | null
  /** 사용자가 정한 표시 이름. 없으면 null. */
  displayName: string | null
  role: UserRole | null
}

export interface LoginOptions {
  /** Google 클라이언트 자격증명이 서버에 설정되어 있는지. */
  googleEnabled: boolean
  /** true 면 첫 로그인이 곧 가입 완료, false 면 관리자 승인 후 이용. */
  autoApproveSignup: boolean
}

export interface AdminUser {
  id: string
  email: string
  displayName: string | null
  role: UserRole
  status: UserStatus
  /** false 면 로그인 수단이 없는 행이다(비밀번호 로그인 제거 이전에 만들어진 계정). */
  googleLinked: boolean
  createdAt: string
  updatedAt: string
}

export interface AppSettings {
  autoApproveSignup: boolean
  updatedAt: string
}

export const roleLabels: Record<UserRole, string> = {
  USER: '일반',
  ADMIN: '관리자',
}

export const userStatusLabels: Record<UserStatus, string> = {
  PENDING: '승인 대기',
  ACTIVE: '활성',
  SUSPENDED: '정지',
  REJECTED: '거절',
}

/** 아바타에 표시할 한 글자. 이름 → 이메일 → '?' 순. */
export function avatarInitial(me: Pick<Me, 'displayName' | 'email'>): string {
  const source = (me.displayName ?? me.email ?? '').trim()
  return source ? source[0].toUpperCase() : '?'
}
