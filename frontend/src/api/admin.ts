import { request } from './http'
import { API } from '../routes'
import type { AdminUser, AppSettings, UserRole, UserStatus } from '../types/auth'

export const listUsers = () => request<AdminUser[]>(API.adminUsers)

export const changeUserStatus = (id: string, status: UserStatus) =>
  request<AdminUser>(API.adminUserStatus(id), {
    method: 'PATCH',
    body: JSON.stringify({ status }),
  })

export const changeUserRole = (id: string, role: UserRole) =>
  request<AdminUser>(API.adminUserRole(id), {
    method: 'PATCH',
    body: JSON.stringify({ role }),
  })

export const changeUserName = (id: string, displayName: string) =>
  request<AdminUser>(API.adminUserName(id), {
    method: 'PATCH',
    body: JSON.stringify({ displayName }),
  })

/** 그 계정이 소유한 기업 정보도 함께 지워진다. */
export const deleteUser = (id: string) =>
  request<void>(API.adminUser(id), { method: 'DELETE' })

export const fetchSettings = () => request<AppSettings>(API.adminSettings)

export const updateAutoApproveSignup = (autoApproveSignup: boolean) =>
  request<AppSettings>(API.adminSettings, {
    method: 'PUT',
    body: JSON.stringify({ autoApproveSignup }),
  })
