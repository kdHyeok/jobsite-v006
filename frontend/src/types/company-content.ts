export type CompanyContentKind = 'NEWS' | 'YOUTUBE'

export interface CompanyContent {
  id: string
  kind: CompanyContentKind
  title: string
  preview: string | null
  /** 뉴스는 신문사, 유튜브는 채널명. */
  source: string | null
  url: string
  createdAt: string
  updatedAt: string
}

export interface CompanyContentPayload {
  kind: CompanyContentKind
  title: string
  preview: string
  source: string
  url: string
}

export const companyContentKindLabels: Record<CompanyContentKind, string> = {
  NEWS: '뉴스',
  YOUTUBE: '유튜브',
}
