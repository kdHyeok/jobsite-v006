import { flushPromises, mount } from '@vue/test-utils'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import App from '../App.vue'
import * as api from '../api/companies'
import type { Company } from '../types/company'

vi.mock('../api/companies', async () => {
  class ApiClientError extends Error {
    fieldErrors = {}
  }
  return {
    ApiClientError,
    listCompanies: vi.fn(),
    createCompany: vi.fn(),
    updateCompany: vi.fn(),
    deleteCompany: vi.fn(),
  }
})

const company: Company = {
  id: '10000000-0000-0000-0000-000000000001',
  name: '루멘 로보틱스 데모',
  industry: '로봇',
  location: '성남',
  websiteUrl: null,
  status: 'PREPARING',
  summary: '합성 기업',
  memo: '메모',
  createdAt: '2026-08-04T00:00:00Z',
  updatedAt: '2026-08-04T00:00:00Z',
}

describe('App', () => {
  beforeEach(() => {
    vi.mocked(api.listCompanies).mockReset().mockResolvedValue([company])
    vi.mocked(api.createCompany).mockReset()
    vi.mocked(api.updateCompany).mockReset()
    vi.mocked(api.deleteCompany).mockReset().mockResolvedValue(undefined)
  })

  it('기업 목록과 선택된 상세를 표시한다', async () => {
    const wrapper = mount(App)
    await flushPromises()

    expect(wrapper.text()).toContain('루멘 로보틱스 데모')
    expect(wrapper.text()).toContain('지원할 기업을 한 화면에서 정리하세요.')
    expect(wrapper.text()).toContain('메모')
  })

  it('확인 후 삭제 API를 호출한다', async () => {
    const wrapper = mount(App)
    await flushPromises()

    await wrapper.get('.detail-actions .danger').trigger('click')
    await wrapper.get('.confirm-card .danger.solid').trigger('click')
    await flushPromises()

    expect(api.deleteCompany).toHaveBeenCalledWith(company.id)
  })

  it('저장 성공 뒤 목록 갱신이 실패해도 form을 닫고 중복 호출하지 않는다', async () => {
    const created = { ...company, id: '10000000-0000-0000-0000-000000000099', name: '새 기업' }
    vi.mocked(api.listCompanies)
      .mockResolvedValueOnce([company])
      .mockRejectedValueOnce(new Error('목록 갱신 실패'))
    vi.mocked(api.createCompany).mockResolvedValue(created)

    const wrapper = mount(App)
    await flushPromises()
    await wrapper.get('.topbar .button.primary').trigger('click')
    await wrapper.get('.modal-card input').setValue('새 기업')
    await wrapper.get('.modal-card form').trigger('submit')
    await flushPromises()

    expect(wrapper.find('.modal-card').exists()).toBe(false)
    expect(api.createCompany).toHaveBeenCalledTimes(1)
    expect(wrapper.text()).toContain('목록 갱신 실패')
  })
})
