export type EmploymentType = 'FULL_TIME' | 'CONTRACT' | 'INTERN' | 'PART_TIME' | 'DISPATCH'

export type ApplicationStage =
  | 'INTERESTED'
  | 'DRAFTING'
  | 'SUBMITTED'
  | 'CODING_TEST'
  | 'INTERVIEW'
  | 'AWAITING_RESULT'

export interface JobPosting {
  id: string
  companyId: string | null
  /** 기업을 연결했으면 기업 이름, 아니면 직접 입력한 고용회사명. */
  companyName: string | null
  position: string
  postingUrl: string | null
  employmentType: EmploymentType
  /** UTC ISO 문자열. null 이면 상시채용. */
  deadlineAt: string | null
  stage: ApplicationStage
  headcount: string | null
  workLocation: string | null
  qualifications: string | null
  responsibilities: string | null
  requiredSkills: string | null
  archived: boolean
  createdAt: string
  updatedAt: string
}

export interface JobPostingPayload {
  companyId: string | null
  companyName: string
  position: string
  postingUrl: string
  employmentType: EmploymentType
  deadlineAt: string | null
  stage: ApplicationStage
  headcount: string
  workLocation: string
  qualifications: string
  responsibilities: string
  requiredSkills: string
}

export const employmentTypeLabels: Record<EmploymentType, string> = {
  FULL_TIME: '정규직',
  CONTRACT: '계약직',
  INTERN: '인턴',
  PART_TIME: '파트타임',
  DISPATCH: '파견직',
}

/** 선언 순서가 진행 정도를 뜻한다. 백엔드 ApplicationStage 와 같은 순서를 유지한다. */
export const stageLabels: Record<ApplicationStage, string> = {
  INTERESTED: '관심',
  DRAFTING: '작성중',
  SUBMITTED: '지원완료',
  CODING_TEST: '코테',
  INTERVIEW: '면접',
  AWAITING_RESULT: '결과대기',
}

export const stageOrder: ApplicationStage[] = [
  'INTERESTED',
  'DRAFTING',
  'SUBMITTED',
  'CODING_TEST',
  'INTERVIEW',
  'AWAITING_RESULT',
]

/**
 * 남은 일수. 서버가 계산해 내려주지 않는다 — 사용자 시계로 계산해야 자정을 넘길 때 어긋나지 않는다.
 * 날짜 경계 기준이라 "오늘 마감"은 0, 내일은 1이다.
 */
export function daysUntil(deadlineAt: string | null, now: Date = new Date()): number | null {
  if (!deadlineAt) return null
  const deadline = new Date(deadlineAt)
  const startOfDeadlineDay = new Date(deadline.getFullYear(), deadline.getMonth(), deadline.getDate())
  const startOfToday = new Date(now.getFullYear(), now.getMonth(), now.getDate())
  return Math.round((startOfDeadlineDay.getTime() - startOfToday.getTime()) / 86_400_000)
}

export function ddayLabel(deadlineAt: string | null, now: Date = new Date()): string {
  const days = daysUntil(deadlineAt, now)
  if (days === null) return '상시'
  if (days === 0) return 'D-DAY'
  return days > 0 ? `D-${days}` : `마감 +${-days}`
}

/** 임박할수록 강한 색. 배지와 목록 점의 색조를 정한다. */
export function ddayTone(
  deadlineAt: string | null,
  now: Date = new Date(),
): 'past' | 'urgent' | 'soon' | 'later' | 'open' {
  const days = daysUntil(deadlineAt, now)
  if (days === null) return 'open'
  if (days < 0) return 'past'
  if (days <= 3) return 'urgent'
  if (days <= 7) return 'soon'
  return 'later'
}

/** 2026-09-10T09:00:00Z -> 2026.09.10 18:00 (사용자 시간대) */
export function formatDeadline(deadlineAt: string | null): string {
  if (!deadlineAt) return '상시채용'
  const date = new Date(deadlineAt)
  const pad = (value: number) => String(value).padStart(2, '0')
  return `${date.getFullYear()}.${pad(date.getMonth() + 1)}.${pad(date.getDate())} `
    + `${pad(date.getHours())}:${pad(date.getMinutes())}`
}

/**
 * datetime-local 입력값(시간대 없음)을 UTC ISO 로. 사용자의 로컬 시간대로 해석한다.
 * 그대로 문자열로 보내면 서버가 UTC 로 오해한다.
 */
export function toUtcIso(localValue: string): string | null {
  if (!localValue) return null
  return new Date(localValue).toISOString()
}

/** UTC ISO -> datetime-local 입력값. 폼을 열 때 되돌린다. */
export function toLocalInput(deadlineAt: string | null): string {
  if (!deadlineAt) return ''
  const date = new Date(deadlineAt)
  const pad = (value: number) => String(value).padStart(2, '0')
  return `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())}`
    + `T${pad(date.getHours())}:${pad(date.getMinutes())}`
}
