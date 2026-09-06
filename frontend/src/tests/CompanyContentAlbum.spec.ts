import { flushPromises, mount } from '@vue/test-utils'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import CompanyContentAlbum from '../components/CompanyContentAlbum.vue'
import * as api from '../api/company-contents'
import type { CompanyContent } from '../types/company-content'

vi.mock('../api/company-contents', async () => {
  class ApiClientError extends Error {
    fieldErrors = {}
  }
  return {
    ApiClientError,
    listCompanyContents: vi.fn(),
    createCompanyContent: vi.fn(),
    updateCompanyContent: vi.fn(),
    deleteCompanyContent: vi.fn(),
  }
})

const companyId = '10000000-0000-0000-0000-000000000001'
const news: CompanyContent = {
  id: '60000000-0000-0000-0000-000000000001',
  kind: 'NEWS',
  title: '신규 서비스 출시',
  preview: '기업 관련 뉴스 본문 요약',
  source: '테크신문',
  url: 'https://example.com/news',
  createdAt: '2026-09-06T00:00:00Z',
  updatedAt: '2026-09-06T00:00:00Z',
}

describe('CompanyContentAlbum', () => {
  beforeEach(() => {
    vi.mocked(api.listCompanyContents).mockReset().mockResolvedValue([news])
    vi.mocked(api.createCompanyContent).mockReset()
    vi.mocked(api.updateCompanyContent).mockReset()
    vi.mocked(api.deleteCompanyContent).mockReset().mockResolvedValue(undefined)
  })

  it('앨범에서 자료를 수정하고 확인 후 삭제한다', async () => {
    const edited = { ...news, title: '수정된 제목' }
    vi.mocked(api.updateCompanyContent).mockResolvedValue(edited)
    const wrapper = mount(CompanyContentAlbum, { props: { companyId } })
    await flushPromises()

    expect(wrapper.get('.content-card').text()).toContain('테크신문')
    await wrapper.get('.content-card__tools button').trigger('click')
    await wrapper.findAll<HTMLInputElement>('#company-content-form input')[1].setValue('수정된 제목')
    await wrapper.get('#company-content-form').trigger('submit')
    await flushPromises()

    expect(api.updateCompanyContent).toHaveBeenCalledWith(companyId, news.id, expect.objectContaining({ title: '수정된 제목' }))
    expect(wrapper.get('.content-card').text()).toContain('수정된 제목')

    await wrapper.get('.content-card__tools .danger').trigger('click')
    await wrapper.get('.confirm-card .danger.solid').trigger('click')
    await flushPromises()

    expect(api.deleteCompanyContent).toHaveBeenCalledWith(companyId, news.id)
    expect(wrapper.find('.content-card').exists()).toBe(false)
  })

  it('유튜브 자료를 새로 저장한다', async () => {
    const video = { ...news, id: '60000000-0000-0000-0000-000000000002', kind: 'YOUTUBE' as const, title: '기업 인터뷰' }
    vi.mocked(api.listCompanyContents).mockResolvedValue([])
    vi.mocked(api.createCompanyContent).mockResolvedValue(video)
    const wrapper = mount(CompanyContentAlbum, { props: { companyId } })
    await flushPromises()

    await wrapper.get('.content-album__head .button').trigger('click')
    await wrapper.get<HTMLSelectElement>('#company-content-form select').setValue('YOUTUBE')
    const inputs = wrapper.findAll<HTMLInputElement>('#company-content-form input')
    await inputs[0].setValue('공식 채널')
    await inputs[1].setValue('기업 인터뷰')
    await inputs[2].setValue('https://youtube.com/watch?v=test')
    await wrapper.get('#company-content-form').trigger('submit')
    await flushPromises()

    expect(api.createCompanyContent).toHaveBeenCalledWith(companyId, expect.objectContaining({ kind: 'YOUTUBE', title: '기업 인터뷰' }))
    expect(wrapper.get('.content-card').text()).toContain('유튜브')
  })
})
