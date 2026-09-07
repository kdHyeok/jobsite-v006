/**
 * 이력서 문서의 모양. 백엔드 ResumeContent 와 키가 1:1 이어야 한다 —
 * Jackson 은 모르는 키를 조용히 버리므로 오타가 나면 저장은 되는데 값이 사라진다.
 * 연월은 사용자가 쓴 표현("2024.03", "현재")을 그대로 두는 문자열이다.
 */
export interface BasicInfo {
  name: string
  phone: string
  birthDate: string
  email: string
  address: string
  portfolioUrl: string
  githubUrl: string
}

/** 섹션 행. 키는 SECTIONS 의 field.key. */
export type Row = Record<string, string>

export type SectionKey =
  | 'educations'
  | 'trainings'
  | 'activities'
  | 'experiences'
  | 'awards'
  | 'certificates'
  | 'skills'
  | 'projects'

export type ResumeContent = { basic: BasicInfo } & Record<SectionKey, Row[]>

export interface ResumeSummary {
  id: string
  name: string
  createdAt: string
  updatedAt: string
}

export interface Resume extends ResumeSummary {
  content: ResumeContent
}

export interface ResumePayload {
  name: string
  content: ResumeContent
}

export type FieldKind = 'text' | 'ym' | 'url' | 'textarea'

export interface FieldConfig {
  key: string
  label: string
  kind: FieldKind
  placeholder?: string
}

export interface SectionConfig {
  key: SectionKey
  label: string
  fields: FieldConfig[]
}

/** 기본정보는 행이 아니라 필드 묶음 하나다. */
export const BASIC_FIELDS: FieldConfig[] = [
  { key: 'name', label: '성명', kind: 'text' },
  { key: 'phone', label: '휴대전화', kind: 'text', placeholder: '010-0000-0000' },
  { key: 'birthDate', label: '생년월일', kind: 'text', placeholder: 'YYYYMMDD' },
  { key: 'email', label: '이메일', kind: 'text' },
  { key: 'address', label: '주소', kind: 'url' },
  { key: 'portfolioUrl', label: '포트폴리오', kind: 'url', placeholder: 'https://' },
  { key: 'githubUrl', label: 'GitHub', kind: 'url', placeholder: 'https://github.com/' },
]

const YM = '예: 2024.03 / 현재'

/** 섹션 순서와 필드 — 사용자가 쓰던 이력서 양식 그대로. 섹션을 더하면 백엔드 ResumeContent 도 함께. */
export const SECTIONS: SectionConfig[] = [
  {
    key: 'educations', label: '학력사항',
    fields: [
      { key: 'school', label: '학교명', kind: 'text' },
      { key: 'major', label: '학과', kind: 'text' },
      { key: 'startYm', label: '입학년월', kind: 'ym', placeholder: YM },
      { key: 'endYm', label: '졸업년월', kind: 'ym', placeholder: YM },
      { key: 'gpa', label: '성적', kind: 'text', placeholder: '예: 3.96 / 4.5' },
    ],
  },
  {
    key: 'trainings', label: '교육이수',
    fields: [
      { key: 'name', label: '교육명', kind: 'text' },
      { key: 'institution', label: '교육기관', kind: 'text' },
      { key: 'startYm', label: '시작년월', kind: 'ym', placeholder: YM },
      { key: 'endYm', label: '종료년월', kind: 'ym', placeholder: YM },
      { key: 'description', label: '교육내용', kind: 'textarea' },
    ],
  },
  {
    key: 'activities', label: '대내외활동',
    fields: [
      { key: 'name', label: '활동명', kind: 'text' },
      { key: 'organizer', label: '주관기관', kind: 'text' },
      { key: 'startYm', label: '시작년월', kind: 'ym', placeholder: YM },
      { key: 'endYm', label: '종료년월', kind: 'ym', placeholder: YM },
      { key: 'description', label: '활동내용', kind: 'textarea' },
    ],
  },
  {
    key: 'experiences', label: '경력사항 (인턴십 등)',
    fields: [
      { key: 'company', label: '기업명', kind: 'text' },
      { key: 'startYm', label: '입사년월', kind: 'ym', placeholder: YM },
      { key: 'endYm', label: '퇴사년월', kind: 'ym', placeholder: YM },
      { key: 'description', label: '직무내용', kind: 'textarea' },
    ],
  },
  {
    key: 'awards', label: '수상내역',
    fields: [
      { key: 'name', label: '수상명', kind: 'text' },
      { key: 'issuer', label: '수여 기관', kind: 'text' },
      { key: 'awardedYm', label: '수상 일자', kind: 'ym', placeholder: YM },
    ],
  },
  {
    key: 'certificates', label: '자격증',
    fields: [
      { key: 'name', label: '자격명', kind: 'text' },
      { key: 'issuer', label: '주관 기관', kind: 'text' },
      { key: 'acquiredYm', label: '취득 일자', kind: 'ym', placeholder: YM },
    ],
  },
  {
    key: 'skills', label: 'SW 역량 (보유기술 및 프로그래밍언어)',
    fields: [
      { key: 'name', label: '보유기술', kind: 'text' },
      { key: 'level', label: '수준', kind: 'text', placeholder: '예: 상 / 중상 / 중' },
      { key: 'description', label: '상세 내용', kind: 'textarea' },
    ],
  },
  {
    key: 'projects', label: '프로젝트 경험',
    fields: [
      { key: 'name', label: '프로젝트명', kind: 'text' },
      { key: 'headcount', label: '참여인원', kind: 'text', placeholder: '예: 5명' },
      { key: 'startYm', label: '시작년월', kind: 'ym', placeholder: YM },
      { key: 'endYm', label: '종료년월', kind: 'ym', placeholder: YM },
      { key: 'summary', label: '개요', kind: 'textarea' },
      { key: 'techStack', label: '기술 환경', kind: 'url', placeholder: '예: Python, PyTorch' },
      { key: 'role', label: '담당 역할', kind: 'url' },
      { key: 'outcome', label: '성과 및 배운점', kind: 'textarea' },
      { key: 'description', label: '상세 내용', kind: 'textarea' },
      { key: 'url', label: '참고 링크', kind: 'url', placeholder: 'https://' },
    ],
  },
]

/** 입력 길이 상한. 백엔드 @Size 와 같다. */
export const MAX_LENGTH: Record<FieldKind, number> = { text: 200, ym: 200, url: 500, textarea: 5000 }

export function emptyRow(section: SectionConfig): Row {
  return Object.fromEntries(section.fields.map((f) => [f.key, '']))
}

export function emptyContent(): ResumeContent {
  const basic = Object.fromEntries(BASIC_FIELDS.map((f) => [f.key, ''])) as unknown as BasicInfo
  const sections = Object.fromEntries(SECTIONS.map((s) => [s.key, [] as Row[]])) as Record<SectionKey, Row[]>
  return { basic, ...sections }
}

/** 서버가 null 로 내려준 칸을 '' 로. 입력칸은 null 을 못 받는다. */
export function fillContent(content: ResumeContent): ResumeContent {
  const empty = emptyContent()
  const basic = {
    ...empty.basic,
    ...Object.fromEntries(Object.entries(content.basic ?? {}).map(([k, v]) => [k, v ?? ''])),
  } as BasicInfo
  const sections = Object.fromEntries(
    SECTIONS.map((s) => [
      s.key,
      (content[s.key] ?? []).map((row) => ({
        ...emptyRow(s),
        ...Object.fromEntries(Object.entries(row).map(([k, v]) => [k, v ?? ''])),
      })),
    ]),
  ) as Record<SectionKey, Row[]>
  return { basic, ...sections }
}
