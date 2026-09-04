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
  position: '백엔드 엔지니어',
  postingUrl: null,
  employmentType: 'FULL_TIME',
  deadlineAt: '2099-09-10T09:00:00Z',
  stage: 'INTERESTED',
  headcount: '0명',
  workLocation: '성남',
  qualifications: null,
  responsibilities: null,
  requiredSkills: null,
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

  it('기업 목록과 선택된 상세를 표시한다', async () => {
    const wrapper = mount(CompanyWorkspace)
    await flushPromises()

    expect(wrapper.text()).toContain('루멘 로보틱스 데모')
    expect(wrapper.text()).toContain('메모')
    expect(wrapper.text()).toContain('1,200억 원')
    expect(wrapper.text()).toContain('2015.03')
  })

  it('상세의 채용정보에 마감 전 공고를 보여준다', async () => {
    const wrapper = mount(CompanyWorkspace)
    await flushPromises()

    expect(api.getCompany).toHaveBeenCalledWith(company.id)
    expect(wrapper.find('.posting-section .posting-card').exists()).toBe(true)
    expect(wrapper.get('.posting-section').text()).toContain('백엔드 엔지니어')
  })

  it('확인 후 삭제 API를 호출한다', async () => {
    const wrapper = mount(CompanyWorkspace)
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

    const wrapper = mount(CompanyWorkspace)
    await flushPromises()
    await wrapper.get('.hero .button.primary').trigger('click')
    await wrapper.get('.modal-card input').setValue('새 기업')
    await wrapper.get('.modal-card form').trigger('submit')
    await flushPromises()

    expect(wrapper.find('.modal-card').exists()).toBe(false)
    expect(api.createCompany).toHaveBeenCalledTimes(1)
    expect(wrapper.text()).toContain('목록 갱신 실패')
  })
})
