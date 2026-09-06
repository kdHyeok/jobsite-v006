export type EmploymentType = 'FULL_TIME' | 'CONTRACT' | 'INTERN' | 'PART_TIME' | 'DISPATCH'

/** 내 참여 상태. 회사 절차(RecruitmentStep)와는 다른 축 — docs/job-postings.md. */
export type ApplicationStatus = 'INTERESTED' | 'DRAFTING' | 'SUBMITTED' | 'CLOSED'

export type StepResult = 'UPCOMING' | 'IN_PROGRESS' | 'PASSED' | 'FAILED'

export interface RecruitmentStep {
  seq: number
  name: string
  result: StepResult
  scheduledAt: string | null
  memo: string | null
}

export interface StepPayload {
  name: string
  result: StepResult
  scheduledAt: string | null
  memo: string
}

/** 공고 응답에 실리는 직무 요약. 상세는 /api/positions/{id}. */
export interface PositionSummary {
  id: string
  name: string
  team: string | null
  headcount: string | null
  workLocation: string | null
}

export interface JobPosting {
  id: string
  companyId: string
  companyName: string | null
  /** 모집 부문. 공채 이름이거나 단일 직무 공고면 그 직무 이름. */
  title: string
  postingUrl: string | null
  employmentType: EmploymentType
  /** UTC ISO. null 이면 상시채용. */
  deadlineAt: string | null
  status: ApplicationStatus
  qualifications: string | null
  targetPositionId: string | null
  steps: RecruitmentStep[]
  positions: PositionSummary[]
  archived: boolean
  createdAt: string
  updatedAt: string
}

export interface JobPostingPayload {
  companyId: string | null
  /** companyId 가 없을 때만. 서버가 기존 기업을 찾거나 새로 만든다. */
  companyName: string
  title: string
  postingUrl: string
  employmentType: EmploymentType
  deadlineAt: string | null
  status: ApplicationStatus
  qualifications: string
  /** id 가 있으면 이름 갱신, 없으면 생성. 빠진 기존 직무는 삭제된다. 비우면 제목 이름의 직무 하나. */
  positions: Array<{ id: string | null; name: string }>
  steps: StepPayload[]
}

export const employmentTypeLabels: Record<EmploymentType, string> = {
  FULL_TIME: '정규직',
  CONTRACT: '계약직',
  INTERN: '인턴',
  PART_TIME: '파트타임',
  DISPATCH: '파견직',
}

export const statusLabels: Record<ApplicationStatus, string> = {
  INTERESTED: '관심',
  DRAFTING: '작성중',
  SUBMITTED: '지원완료',
  CLOSED: '종료',
}

export const statusTone: Record<ApplicationStatus, string> = {
  INTERESTED: 'neutral',
  DRAFTING: 'warning',
  SUBMITTED: 'primary',
  CLOSED: 'neutral',
}

export const stepResultLabels: Record<StepResult, string> = {
  UPCOMING: '예정',
  IN_PROGRESS: '진행 중',
  PASSED: '통과',
  FAILED: '탈락',
}

/** 절차 노드를 누르면 이 순서로 돈다. 탈락 뒤에는 예정으로 돌아간다. */
const STEP_CYCLE: StepResult[] = ['UPCOMING', 'IN_PROGRESS', 'PASSED', 'FAILED']
export function nextStepResult(current: StepResult): StepResult {
  return STEP_CYCLE[(STEP_CYCLE.indexOf(current) + 1) % STEP_CYCLE.length]
}

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

/** 임박할수록 강한 색. */
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

/** datetime-local 입력값(시간대 없음)을 UTC ISO 로. 사용자의 로컬 시간대로 해석한다. */
export function toUtcIso(localValue: string): string | null {
  if (!localValue) return null
  return new Date(localValue).toISOString()
}

/** UTC ISO -> datetime-local 입력값. */
export function toLocalInput(deadlineAt: string | null): string {
  if (!deadlineAt) return ''
  const date = new Date(deadlineAt)
  const pad = (value: number) => String(value).padStart(2, '0')
  return `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())}`
    + `T${pad(date.getHours())}:${pad(date.getMinutes())}`
}
