import { mount } from '@vue/test-utils'
import { describe, expect, it } from 'vitest'
import PositionDetail from '../components/PositionDetail.vue'
import type { Position, ReferenceItem } from '../types/position'

const reference: ReferenceItem = {
  id: '50000000-0000-0000-0000-000000000001',
  kind: 'SENIOR_INTERVIEW',
  title: '검색팀 3년차 인터뷰',
  url: 'https://example.com/interview',
  memo: '팀 분위기 참고',
  relatedPositionId: null,
  createdAt: '2026-09-01T00:00:00Z',
  updatedAt: '2026-09-01T00:00:00Z',
}

const position: Position = {
  id: '40000000-0000-0000-0000-000000000001',
  postingId: '30000000-0000-0000-0000-000000000001',
  postingTitle: '2026 신입 공채',
  companyId: '10000000-0000-0000-0000-000000000001',
  companyName: '루멘 로보틱스 데모',
  deadlineAt: '2099-09-10T09:00:00Z',
  status: 'INTERESTED',
  archived: false,
  name: '백엔드 개발',
  team: null,
  role: null,
  responsibilities: null,
  impact: null,
  growth: null,
  experience: null,
  requiredSkills: null,
  preferredSkills: null,
  headcount: null,
  workLocation: null,
  techStack: [],
  references: [reference],
  createdAt: '2026-09-01T00:00:00Z',
  updatedAt: '2026-09-01T00:00:00Z',
}

const mountDetail = () =>
  mount(PositionDetail, {
    props: { position, allReferences: [reference], allPositions: [position], busy: false },
  })

describe('PositionDetail 참고 정보', () => {
  it('요구·우대 역량을 성장 방향·취득 경험보다 먼저 보여준다', () => {
    const labels = mountDetail().findAll('.detail-section > .label').map((node) => node.text())
    expect(labels.indexOf('요구 역량')).toBeLessThan(labels.indexOf('성장 방향'))
    expect(labels.indexOf('우대 역량')).toBeLessThan(labels.indexOf('취득 경험'))
  })

  it('카드에 수정·떼기·삭제가 있다', () => {
    const wrapper = mountDetail()

    expect(wrapper.findAll('.strip-card__tools button').map((b) => b.text()))
      .toEqual(['수정', '떼기', '삭제'])
  })

  /** 수정은 만들기와 같은 폼을 쓰고, 저장하면 id 와 함께 update 를 올린다. */
  it('수정을 누르면 값이 채워진 폼이 열리고 저장 시 update 를 올린다', async () => {
    const wrapper = mountDetail()

    await wrapper.findAll('.strip-card__tools button')[0].trigger('click')

    const title = wrapper.get<HTMLInputElement>('.picker input[maxlength="200"]')
    expect(title.element.value).toBe('검색팀 3년차 인터뷰')

    await title.setValue('검색팀 5년차 인터뷰')
    await wrapper.get('.picker').trigger('submit')

    expect(wrapper.emitted('update')?.[0]?.[0]).toBe(reference.id)
    expect(wrapper.emitted('update')?.[0]?.[1]).toMatchObject({
      title: '검색팀 5년차 인터뷰',
      kind: 'SENIOR_INTERVIEW',
      url: 'https://example.com/interview',
    })
    expect(wrapper.emitted('create')).toBeUndefined()
  })

  /** 떼기는 이 직무에서만, 삭제는 참고 정보 자체 — 부모가 확인을 받는다. */
  it('떼기와 삭제는 서로 다른 이벤트를 올린다', async () => {
    const wrapper = mountDetail()
    const tools = wrapper.findAll('.strip-card__tools button')

    await tools[1].trigger('click')
    await tools[2].trigger('click')

    expect(wrapper.emitted('detach')?.[0]).toEqual([reference.id])
    expect(wrapper.emitted('remove')?.[0]?.[0]).toMatchObject({ id: reference.id })
  })
})
