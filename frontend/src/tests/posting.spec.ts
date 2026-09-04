import { describe, expect, it } from 'vitest'
import { daysUntil, ddayLabel, ddayTone, formatDeadline, toLocalInput, toUtcIso } from '../types/posting'

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

  it('마감 시각을 2026.09.10 18:00 꼴로 보여준다', () => {
    expect(formatDeadline(new Date(2026, 8, 10, 18, 0).toISOString())).toBe('2026.09.10 18:00')
  })
})
