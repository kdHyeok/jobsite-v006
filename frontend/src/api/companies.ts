import type { ApiErrorBody, Company, CompanyPayload } from '../types/company'

export class ApiClientError extends Error {
  public readonly status: number
  public readonly code: string
  public readonly fieldErrors: Record<string, string>

  constructor(
    status: number,
    code: string,
    message: string,
    fieldErrors: Record<string, string> = {},
  ) {
    super(message)
    this.name = 'ApiClientError'
    this.status = status
    this.code = code
    this.fieldErrors = fieldErrors
  }
}

async function request<T>(path: string, init?: RequestInit): Promise<T> {
  const response = await fetch(path, {
    ...init,
    headers: {
      'Content-Type': 'application/json',
      ...init?.headers,
    },
  })

  if (!response.ok) {
    let body: Partial<ApiErrorBody> = {}
    try {
      body = (await response.json()) as Partial<ApiErrorBody>
    } catch {
      // JSON이 아닌 proxy 오류도 사용자 메시지로 안전하게 변환한다.
    }
    throw new ApiClientError(
      response.status,
      body.code ?? 'REQUEST_FAILED',
      body.message ?? '요청을 처리하지 못했습니다.',
      body.fieldErrors ?? {},
    )
  }

  if (response.status === 204) {
    return undefined as T
  }
  return (await response.json()) as T
}

export const listCompanies = () => request<Company[]>('/api/companies')
export const getCompany = (id: string) => request<Company>(`/api/companies/${id}`)
export const createCompany = (payload: CompanyPayload) =>
  request<Company>('/api/companies', { method: 'POST', body: JSON.stringify(payload) })
export const updateCompany = (id: string, payload: CompanyPayload) =>
  request<Company>(`/api/companies/${id}`, { method: 'PUT', body: JSON.stringify(payload) })
export const deleteCompany = (id: string) =>
  request<void>(`/api/companies/${id}`, { method: 'DELETE' })
