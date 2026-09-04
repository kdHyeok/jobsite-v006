import type { JobPosting } from './posting'

export type CompanySize = 'STARTUP' | 'SMALL' | 'MEDIUM' | 'LARGE' | 'PUBLIC'

export interface Company {
  id: string
  name: string
  websiteUrl: string | null
  /** 업종 다중값. 입력 순서를 유지한다. */
  industries: string[]
  companySize: CompanySize | null
  /** 원 단위. 표기는 formatRevenue 가 만든다. */
  annualRevenue: number | null
  employeeCount: number | null
  address: string | null
  /** YYYY-MM-DD. 입력은 연월만 받고 1일로 저장된다. */
  foundedOn: string | null
  summary: string | null
  memo: string | null
  /** 사용자 입력이 아니라 공고에서 채워진다. 목록 응답에서는 비어 있다. */
  openPostings: JobPosting[]
  createdAt: string
  updatedAt: string
}

export interface CompanyPayload {
  name: string
  websiteUrl: string
  industries: string[]
  companySize: CompanySize | null
  annualRevenue: number | null
  employeeCount: number | null
  address: string
  foundedOn: string | null
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

export const companySizeLabels: Record<CompanySize, string> = {
  STARTUP: '스타트업',
  SMALL: '중소기업',
  MEDIUM: '중견기업',
  LARGE: '대기업',
  PUBLIC: '공기업',
}

/** 매출액을 조/억 단위로 읽기 쉽게. */
export function formatRevenue(won: number | null): string {
  if (won === null) return '—'
  const trillion = 1_000_000_000_000
  const hundredMillion = 100_000_000
  if (won >= trillion) {
    const value = won / trillion
    return `${Number(value.toFixed(value < 10 ? 1 : 0))}조 원`
  }
  if (won >= hundredMillion) {
    return `${Math.round(won / hundredMillion).toLocaleString('ko-KR')}억 원`
  }
  return `${won.toLocaleString('ko-KR')}원`
}

/** 2015-03-01 -> 2015.03 */
export function formatFoundedOn(value: string | null): string {
  if (!value) return '—'
  const [year, month] = value.split('-')
  return `${year}.${month}`
}
