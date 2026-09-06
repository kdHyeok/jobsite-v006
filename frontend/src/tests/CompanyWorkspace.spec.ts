import { flushPromises, mount } from '@vue/test-utils'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import CompanyWorkspace from '../components/CompanyWorkspace.vue'
import * as api from '../api/companies'
import type { Company } from '../types/company'
import type { JobPosting } from '../types/posting'

vi.mock('../api/companies', async () => {
  class ApiClientError extends Error {
    fieldErrors = {}
  }
  return {
    ApiClientError,
    listCompanies: vi.fn(),
    getCompany: vi.fn(),
    createCompany: vi.fn(),
    updateCompany: vi.fn(),
    deleteCompany: vi.fn(),
  }
})

const posting: JobPosting = {
  id: '30000000-0000-0000-0000-000000000001',
  companyId: '10000000-0000-0000-0000-000000000001',
  companyName: '루멘 로보틱스 데모',
  title: '백엔드 엔지니어',
  postingUrl: null,
  employmentType: 'FULL_TIME',
  deadlineAt: '2099-09-10T09:00:00Z',
  status: 'INTERESTED',
  qualifications: null,
  targetPositionId: null,
  steps: [],
  positions: [{ id: '40000000-0000-0000-0000-000000000001', name: '백엔드 엔지니어', team: null, headcount: null, workLocation: null }],
  archived: false,
  createdAt: '2026-08-04T00:00:00Z',
  updatedAt: '2026-08-04T00:00:00Z',
}

const company: Company = {
  id: '10000000-0000-0000-0000-000000000001',
  name: '루멘 로보틱스 데모',
  websiteUrl: null,
  industries: ['로봇'],
  companySize: 'MEDIUM',
  annualRevenue: 120_000_000_000,
  employeeCount: 420,
  address: '성남',
  foundedOn: '2015-03-01',
  summary: '합성 기업',
  memo: '메모',
  openPostings: [],
  createdAt: '2026-08-04T00:00:00Z',
  updatedAt: '2026-08-04T00:00:00Z',
}

describe('CompanyWorkspace', () => {
  beforeEach(() => {
    vi.mocked(api.listCompanies).mockReset().mockResolvedValue([company])
    // 목록에는 openPostings 가 비어 있고 상세에서만 채워진다.
    vi.mocked(api.getCompany).mockReset().mockResolvedValue({ ...company, openPostings: [posting] })
    vi.mocked(api.createCompany).mockReset()
    vi.mocked(api.updateCompany).mockReset()
    vi.mocked(api.deleteCompany).mockReset().mockResolvedValue(undefined)
  })

  /** 카드에는 핵심만 — 매출액 같은 상세는 드로어를 열어야 나온다. */
  it('목록 카드에는 이름과 업종만 보여준다', async () => {
    const wrapper = mount(CompanyWorkspace)
    await flushPromises()

    const card = wrapper.get('.card')
    expect(card.text()).toContain('루멘 로보틱스 데모')
    expect(card.text()).toContain('로봇')
    expect(card.text()).not.toContain('1,200억 원')
    expect(wrapper.find('.drawer').exists()).toBe(false)
    expect(api.getCompany).not.toHaveBeenCalled()
  })

  it('카드를 누르면 드로어에 상세와 마감 전 공고가 뜬다', async () => {
    const wrapper = mount(CompanyWorkspace)
    await flushPromises()

    await wrapper.get('.card').trigger('click')
    await flushPromises()

    expect(api.getCompany).toHaveBeenCalledWith(company.id)
    const drawer = wrapper.get('.drawer')
    expect(drawer.text()).toContain('1,200억 원')
    expect(drawer.text()).toContain('2015.03')
    expect(drawer.text()).toContain('백엔드 엔지니어')
  })

  /** 종속 데이터를 더블클릭하면 그 데이터의 화면이 열린다. 라우팅은 App 이 한다. */
  it('상세의 채용정보 행을 더블클릭하면 공고 열기를 요청한다', async () => {
    const wrapper = mount(CompanyWorkspace)
    await flushPromises()
    await wrapper.get('.card').trigger('click')
    await flushPromises()

    const row = wrapper.get('.drawer .mini-row')
    expect(row.element.tagName).toBe('BUTTON')

    await row.trigger('dblclick')

    expect(wrapper.emitted('openPosting')?.[0]).toEqual([posting.id])
  })

  it('드로어에서 수정을 누르면 같은 자리에서 폼으로 바뀐다', async () => {
    const wrapper = mount(CompanyWorkspace)
    await flushPromises()
    await wrapper.get('.card').trigger('click')
    await flushPromises()

    await wrapper.get('.drawer__actions .secondary').trigger('click')

    expect(wrapper.find('#company-form').exists()).toBe(true)
    expect(wrapper.findAll('.drawer')).toHaveLength(1)
    expect(wrapper.get<HTMLInputElement>('#company-form input').element.value)
      .toBe('루멘 로보틱스 데모')
  })

  it('확인 후 삭제 API를 호출한다', async () => {
    const wrapper = mount(CompanyWorkspace)
    await flushPromises()
    await wrapper.get('.card').trigger('click')
    await flushPromises()

    await wrapper.get('.drawer__actions .danger').trigger('click')
    await wrapper.get('.confirm-card .danger.solid').trigger('click')
    await flushPromises()

    expect(api.deleteCompany).toHaveBeenCalledWith(company.id)
  })

  it('저장 성공 뒤 목록 갱신이 실패해도 폼을 닫고 중복 호출하지 않는다', async () => {
    const created = { ...company, id: '10000000-0000-0000-0000-000000000099', name: '새 기업' }
    vi.mocked(api.listCompanies)
      .mockResolvedValueOnce([company])
      .mockRejectedValueOnce(new Error('목록 갱신 실패'))
    vi.mocked(api.createCompany).mockResolvedValue(created)

    const wrapper = mount(CompanyWorkspace)
    await flushPromises()
    await wrapper.get('.page-head .button.primary').trigger('click')
    await wrapper.get('#company-form input').setValue('새 기업')
    await wrapper.get('#company-form').trigger('submit')
    await flushPromises()

    expect(wrapper.find('#company-form').exists()).toBe(false)
    expect(api.createCompany).toHaveBeenCalledTimes(1)
    expect(wrapper.text()).toContain('목록 갱신 실패')
  })
})
