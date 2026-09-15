import { flushPromises, mount } from '@vue/test-utils'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import SelfIntroductionBoard from '../components/SelfIntroductionBoard.vue'
import * as api from '../api/self-introductions'
import { listResumes } from '../api/resumes'
import { highlightMatches } from '../utils/highlight'

vi.mock('../api/self-introductions', () => ({
  listSelfIntroductions: vi.fn(),
  createSelfIntroduction: vi.fn(),
  updateSelfIntroduction: vi.fn(),
  deleteSelfIntroduction: vi.fn(),
}))

vi.mock('../api/resumes', () => ({ listResumes: vi.fn() }))

const item = {
  id: '70000000-0000-0000-0000-000000000001',
  resumeId: '80000000-0000-0000-0000-000000000001',
  question: '협업 경험을 설명해 주세요.',
  answer: '역할을 나누고 진행했습니다.',
  createdAt: '2026-09-16T00:00:00Z',
  updatedAt: '2026-09-16T00:00:00Z',
}

describe('자기소개 검색 하이라이트', () => {
  beforeEach(() => {
    vi.mocked(api.listSelfIntroductions).mockReset().mockResolvedValue([item])
    vi.mocked(listResumes).mockReset().mockResolvedValue([{
      id: item.resumeId,
      name: '백엔드 이력서',
      createdAt: item.createdAt,
      updatedAt: item.updatedAt,
    }])
  })

  it('검색어만 표시하고 HTML은 문자열로 남긴다', () => {
    expect(highlightMatches('<b>협업</b> 경험', '협업')).toEqual([
      { text: '<b>', matched: false },
      { text: '협업', matched: true },
      { text: '</b> 경험', matched: false },
    ])
  })

  it('문항을 더블클릭하면 기존 값으로 편집기를 연다', async () => {
    const wrapper = mount(SelfIntroductionBoard)
    await flushPromises()

    await wrapper.get('.intro-card h2').trigger('dblclick')

    expect(wrapper.get('.intro-editor h2').text()).toBe('문항 수정')
    expect(wrapper.get<HTMLTextAreaElement>('.intro-editor textarea').element.value).toBe(item.question)
  })
})
