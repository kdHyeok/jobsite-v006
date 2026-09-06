import { request } from './http'
import { API } from '../routes'
import type { ReferenceItem, ReferencePayload } from '../types/position'

export { ApiClientError } from './http'

export const listReferences = (q = '') =>
  request<ReferenceItem[]>(q ? `${API.references}?q=${encodeURIComponent(q)}` : API.references)

export const createReference = (payload: ReferencePayload) =>
  request<ReferenceItem>(API.references, { method: 'POST', body: JSON.stringify(payload) })

export const updateReference = (id: string, payload: ReferencePayload) =>
  request<ReferenceItem>(API.reference(id), { method: 'PUT', body: JSON.stringify(payload) })

export const deleteReference = (id: string) =>
  request<void>(API.reference(id), { method: 'DELETE' })
