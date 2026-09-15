import { refreshSession, request } from './http'
import { API } from '../routes'
import type { LoginOptions, Me } from '../types/auth'

export { ApiClientError } from './http'

export const fetchMe = async () => {
  const me = await request<Me>(API.me)
  return !me.authenticated && await refreshSession() ? request<Me>(API.me) : me
}

export const fetchLoginOptions = () => request<LoginOptions>(API.loginOptions)

/** 빈 문자열을 보내면 이름이 지워진다. */
export const updateMyName = (displayName: string) =>
  request<Me>(API.me, { method: 'PATCH', body: JSON.stringify({ displayName }) })

export const logout = () => request<void>(API.logout, { method: 'POST' })
