import { request } from './http'
import { API } from '../routes'
import type { Company, CompanyPayload } from '../types/company'

export { ApiClientError } from './http'

export const listCompanies = () => request<Company[]>(API.companies)
export const getCompany = (id: string) => request<Company>(API.company(id))
export const createCompany = (payload: CompanyPayload) =>
  request<Company>(API.companies, { method: 'POST', body: JSON.stringify(payload) })
export const updateCompany = (id: string, payload: CompanyPayload) =>
  request<Company>(API.company(id), { method: 'PUT', body: JSON.stringify(payload) })
export const deleteCompany = (id: string) =>
  request<void>(API.company(id), { method: 'DELETE' })
