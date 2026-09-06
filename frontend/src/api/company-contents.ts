import { request } from './http'
import { API } from '../routes'
import type { CompanyContent, CompanyContentPayload } from '../types/company-content'

export { ApiClientError } from './http'

export const listCompanyContents = (companyId: string) =>
  request<CompanyContent[]>(API.companyContents(companyId))
export const createCompanyContent = (companyId: string, payload: CompanyContentPayload) =>
  request<CompanyContent>(API.companyContents(companyId), { method: 'POST', body: JSON.stringify(payload) })
export const updateCompanyContent = (companyId: string, id: string, payload: CompanyContentPayload) =>
  request<CompanyContent>(API.companyContent(companyId, id), { method: 'PUT', body: JSON.stringify(payload) })
export const deleteCompanyContent = (companyId: string, id: string) =>
  request<void>(API.companyContent(companyId, id), { method: 'DELETE' })
