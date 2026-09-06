import { describe, expect, it } from 'vitest'
import { daysUntil, ddayLabel, ddayTone, formatDeadline, groupPostingsByKanban, nextStepResult, postingDropTarget, postingTabCounts, toLocalInput, toUtcIso } from '../types/posting'
import type { ApplicationStatus, JobPosting } from '../types/posting'

// 서버는 D-day 를 내려주지 않는다. 이 계산이 정렬 표기의 전부라서 여기만 지키면 된다.
const now = new Date(2026, 8, 4, 10, 0) // 2026-09-04 10:00 로컬

describe('D-day', () => {
  it('날짜 경계로 센다 — 같은 날은 시각과 무관하게 D-DAY', () => {
    expect(daysUntil(new Date(2026, 8, 4, 23, 59).toISOString(), now)).toBe(0)
    expect(ddayLabel(new Date(2026, 8, 4, 0, 1).toISOString(), now)).toBe('D-DAY')
  })

  it('남은 날과 지난 날을 구분해 표기한다', () => {
    expect(ddayLabel(new Date(2026, 8, 10, 18, 0).toISOString(), now)).toBe('D-6')
    expect(ddayLabel(new Date(2026, 8, 1, 18, 0).toISOString(), now)).toBe('마감 +3')
    expect(ddayLabel(null, now)).toBe('상시')
  })

  it('임박 정도에 따라 색조를 나눈다', () => {
    expect(ddayTone(new Date(2026, 8, 1).toISOString(), now)).toBe('past')
    expect(ddayTone(new Date(2026, 8, 7).toISOString(), now)).toBe('urgent')
    expect(ddayTone(new Date(2026, 8, 10).toISOString(), now)).toBe('soon')
    expect(ddayTone(new Date(2026, 9, 10).toISOString(), now)).toBe('later')
    expect(ddayTone(null, now)).toBe('open')
  })
})

describe('마감 시각 변환', () => {
  it('datetime-local 값을 로컬 시간대로 해석해 UTC 로 보낸다', () => {
    const iso = toUtcIso('2026-09-10T18:00')
    expect(iso).toBe(new Date(2026, 8, 10, 18, 0).toISOString())
    // 왕복해도 같은 입력값으로 돌아온다.
    expect(toLocalInput(iso)).toBe('2026-09-10T18:00')
  })

  it('빈 값은 상시채용으로 둔다', () => {
    expect(toUtcIso('')).toBeNull()
    expect(toLocalInput(null)).toBe('')
    expect(formatDeadline(null)).toBe('상시채용')
  })

  it('마감 시각을 년월일 오전·오후 시분으로 보여준다', () => {
    expect(formatDeadline(new Date(2026, 8, 10, 18, 0).toISOString())).toBe('2026년 9월 10일 오후 6시 00분')
  })
})

describe('절차 완료 전환', () => {
  it('예정과 완료만 번갈아 바꾼다', () => {
    expect(nextStepResult('UPCOMING')).toBe('PASSED')
    expect(nextStepResult('PASSED')).toBe('UPCOMING')
  })
})

describe('지원 상태 칸반', () => {
  const posting = (status: ApplicationStatus): JobPosting => ({
    id: status, companyId: 'company', companyName: '회사', title: status, postingUrl: null,
    employmentType: 'FULL_TIME', deadlineAt: null, status, qualifications: null,
    targetPositionId: null, steps: [], positions: [], archived: false,
    createdAt: '', updatedAt: '',
  })

  it('관심·진행 중·보관함을 요청한 상태 열로 나눈다', () => {
    const interested = groupPostingsByKanban([posting('INTERESTED'), posting('ACCEPTED')], 'interested')
    expect(interested.find((column) => column.label === '관심')?.postings).toHaveLength(1)
    expect(interested.flatMap((column) => column.postings)).toHaveLength(1)

    const progress = groupPostingsByKanban([posting('INTERESTED'), posting('ACCEPTED')], 'progress')
    expect(progress.find((column) => column.label === '합격')?.postings).toHaveLength(1)

    const archived = groupPostingsByKanban([posting('DRAFTING'), posting('WRITTEN_TEST_REJECTED')], 'archived')
    expect(archived.find((column) => column.label === '미지원')?.postings).toHaveLength(1)
    expect(archived.find((column) => column.label === '필기 탈락')?.postings).toHaveLength(1)
  })

  it('열 드롭과 탭 드롭의 상태·보관 값을 정한다', () => {
    expect(postingDropTarget('INTERESTED', 'interested', 'drafting')).toEqual({ status: 'DRAFTING', archived: false })
    expect(postingDropTarget('DRAFTING', 'progress')).toEqual({ status: 'SUBMITTED', archived: false })
    expect(postingDropTarget('SUBMITTED', 'archived')).toEqual({ status: 'DOCUMENT_REJECTED', archived: true })
    expect(postingDropTarget('WRITTEN_TEST_PREP', 'archived')).toEqual({ status: 'WRITTEN_TEST_REJECTED', archived: true })
    expect(postingDropTarget('INTERVIEW_PREP', 'archived')).toEqual({ status: 'INTERVIEW_REJECTED', archived: true })
    expect(postingDropTarget('DRAFTING', 'archived')).toEqual({ status: 'DRAFTING', archived: true })
  })

  it('세 탭의 공고 수를 상태와 보관 목록으로 나눈다', () => {
    const counts = postingTabCounts(
      [posting('INTERESTED'), posting('DRAFTING'), posting('SUBMITTED'), posting('ACCEPTED')],
      [posting('DOCUMENT_REJECTED')],
    )
    expect(counts).toEqual({ interested: 2, progress: 2, archived: 1 })
  })
})
