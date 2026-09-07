import { describe, expect, it } from 'vitest'
import { mount } from '@vue/test-utils'
import ResumeSection from '../components/ResumeSection.vue'
import { SECTIONS, emptyContent, emptyRow, fillContent } from '../types/resume'
import type { ResumeContent } from '../types/resume'

/** 백엔드 ResumeContent 와 키가 어긋나면 값이 조용히 사라진다. 여기서 모양을 고정한다. */
describe('이력서 문서 모양', () => {
  it('빈 문서는 모든 섹션을 빈 배열로 갖는다', () => {
    const content = emptyContent()
    expect(Object.keys(content).sort()).toEqual(['basic', ...SECTIONS.map((s) => s.key)].sort())
    for (const section of SECTIONS) expect(content[section.key]).toEqual([])
    expect(content.basic.name).toBe('')
  })

  it('서버의 null 칸을 빈 문자열로 채우고 빠진 키를 보탠다', () => {
    const partial = {
      basic: { name: '테스트', phone: null },
      educations: [{ school: '테스트 대학교', gpa: null }],
    } as unknown as ResumeContent
    const filled = fillContent(partial)
    expect(filled.basic.phone).toBe('')
    expect(filled.educations[0]).toEqual({ ...emptyRow(SECTIONS[0]), school: '테스트 대학교' })
    expect(filled.projects).toEqual([])
  })
})

describe('ResumeSection', () => {
  const section = SECTIONS.find((s) => s.key === 'certificates')!
  const rows = [
    { name: 'SQLD', issuer: '기관 A', acquiredYm: '2026.03' },
    { name: '정보처리기사', issuer: '기관 B', acquiredYm: '2025.06' },
  ]
  const mountSection = () => mount(ResumeSection, { props: { section, rows } })

  it('행 추가는 빈 행을 뒤에 붙인 새 배열을 올린다', async () => {
    const wrapper = mountSection()
    await wrapper.get('.resume-section__head button').trigger('click')

    const next = wrapper.emitted('update:rows')?.[0]?.[0] as Record<string, string>[]
    expect(next).toHaveLength(3)
    expect(next[2]).toEqual(emptyRow(section))
    expect(rows).toHaveLength(2) // 원본은 건드리지 않는다
  })

  it('↓ 는 순서를 바꾸고 × 는 행을 지운다', async () => {
    const wrapper = mountSection()
    const firstRowButtons = wrapper.findAll('.resume-row')[0].findAll('.repeater__move button')

    await firstRowButtons[1].trigger('click') // ↓
    expect((wrapper.emitted('update:rows')?.[0]?.[0] as typeof rows).map((r) => r.name))
      .toEqual(['정보처리기사', 'SQLD'])

    await firstRowButtons[2].trigger('click') // ×
    expect((wrapper.emitted('update:rows')?.[1]?.[0] as typeof rows).map((r) => r.name))
      .toEqual(['정보처리기사'])
  })

  it('칸을 고치면 그 행만 바뀐 새 배열을 올린다', async () => {
    const wrapper = mountSection()
    await wrapper.findAll('.resume-row')[1].get('input').setValue('ADsP')

    const next = wrapper.emitted('update:rows')?.[0]?.[0] as typeof rows
    expect(next[1].name).toBe('ADsP')
    expect(next[0]).toEqual(rows[0])
  })
})
