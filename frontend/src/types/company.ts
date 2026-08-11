export type CompanyStatus = 'INTERESTED' | 'PREPARING' | 'APPLIED' | 'ARCHIVED'

export interface Company {
  id: string
  name: string
  industry: string | null
  location: string | null
  websiteUrl: string | null
  status: CompanyStatus
  summary: string | null
  memo: string | null
  createdAt: string
  updatedAt: string
}

export interface CompanyPayload {
  name: string
  industry: string
  location: string
  websiteUrl: string
  status: CompanyStatus
  summary: string
  memo: string
}

export interface ApiErrorBody {
  timestamp: string
  status: number
  code: string
  message: string
  fieldErrors: Record<string, string>
}

export const statusLabels: Record<CompanyStatus, string> = {
  INTERESTED: '관심',
  PREPARING: '지원 준비',
  APPLIED: '지원 완료',
  ARCHIVED: '보관',
}
