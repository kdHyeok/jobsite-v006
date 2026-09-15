import { request } from './http'
import { API } from '../routes'
import type { AdminRequest, AdminRequestForAdmin, AdminRequestInput } from '../types/admin-request'

export const listMyAdminRequests = () => request<AdminRequest[]>(API.requests)
export const createAdminRequest = (input: AdminRequestInput) =>
  request<AdminRequest>(API.requests, { method: 'POST', body: JSON.stringify(input) })
export const updateAdminRequest = (id: string, input: AdminRequestInput) =>
  request<AdminRequest>(API.request(id), { method: 'PUT', body: JSON.stringify(input) })
export const deleteMyAdminRequest = (id: string) => request<void>(API.request(id), { method: 'DELETE' })
export const readAdminFeedback = (id: string) =>
  request<AdminRequest>(API.requestFeedbackRead(id), { method: 'PATCH' })

export const listAdminRequests = () => request<AdminRequestForAdmin[]>(API.adminRequests)
export const saveAdminFeedback = (id: string, feedback: string) =>
  request<AdminRequestForAdmin>(API.adminRequestFeedback(id), {
    method: 'PUT', body: JSON.stringify({ feedback }),
  })
export const deleteAdminFeedback = (id: string) =>
  request<AdminRequestForAdmin>(API.adminRequestFeedback(id), { method: 'DELETE' })
export const deleteAdminRequest = (id: string) => request<void>(API.adminRequest(id), { method: 'DELETE' })
