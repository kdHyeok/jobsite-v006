import { flushPromises, mount } from '@vue/test-utils'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import App from '../App.vue'
import * as auth from '../api/auth'
import { GOOGLE_LOGIN_URL, ROUTES } from '../routes'
import type { Me } from '../types/auth'

vi.mock('../api/auth', () => {
  class ApiClientError extends Error {
    fieldErrors = {}
  }
  return {
    ApiClientError,
    fetchMe: vi.fn(),
    fetchLoginOptions: vi.fn(),
    updateMyName: vi.fn(),
    logout: vi.fn(),
  }
})

vi.mock('../api/companies', () => {
  class ApiClientError extends Error {
    fieldErrors = {}
  }
  return {
    ApiClientError,
    listCompanies: vi.fn(() => Promise.resolve([])),
    getCompany: vi.fn(),
    createCompany: vi.fn(),
    updateCompany: vi.fn(),
    deleteCompany: vi.fn(),
  }
})

vi.mock('../api/postings', () => {
  class ApiClientError extends Error {
    fieldErrors = {}
  }
  return {
    ApiClientError,
    listPostings: vi.fn(() => Promise.resolve([])),
    listArchivedPostings: vi.fn(() => Promise.resolve([])),
    createPosting: vi.fn(),
    updatePosting: vi.fn(),
    changePostingStage: vi.fn(),
    setPostingArchived: vi.fn(),
    deletePosting: vi.fn(),
  }
})

vi.mock('../api/admin', () => ({
  listUsers: vi.fn(() => Promise.resolve([])),
  fetchSettings: vi.fn(() =>
    Promise.resolve({ autoApproveSignup: false, updatedAt: '2026-09-04T00:00:00Z' }),
  ),
  changeUserStatus: vi.fn(),
  changeUserRole: vi.fn(),
  changeUserName: vi.fn(),
  deleteUser: vi.fn(),
  updateAutoApproveSignup: vi.fn(),
}))

const anonymous: Me = { authenticated: false, id: null, email: null, displayName: null, role: null }
const member: Me = {
  authenticated: true,
  id: '20000000-0000-0000-0000-000000000001',
  email: 'member@example.com',
  displayName: '홍길동',
  role: 'USER',
}
const admin: Me = {
  authenticated: true,
  id: '20000000-0000-0000-0000-0000000000ad',
  email: 'admin@example.com',
  displayName: null,
  role: 'ADMIN',
}

function setPath(path: string) {
  window.history.replaceState({}, '', path)
}

describe('App 접근 제어', () => {
  beforeEach(() => {
    setPath(ROUTES.home)
    vi.mocked(auth.fetchLoginOptions).mockReset().mockResolvedValue({
      googleEnabled: true,
      autoApproveSignup: false,
    })
    vi.mocked(auth.fetchMe).mockReset()
    vi.mocked(auth.updateMyName).mockReset()
  })

  it('비로그인 상태에서는 Google 로그인 버튼만 보여준다', async () => {
    vi.mocked(auth.fetchMe).mockResolvedValue(anonymous)

    const wrapper = mount(App)
    await flushPromises()

    expect(wrapper.find('.auth-card').exists()).toBe(true)
    expect(wrapper.find('.google-button').attributes('href')).toBe(GOOGLE_LOGIN_URL)
    // 비밀번호 로그인은 존재하지 않는다.
    expect(wrapper.find('input[type="password"]').exists()).toBe(false)
    expect(wrapper.find('.avatar').exists()).toBe(false)
  })

  it('Google 자격증명이 없으면 안내만 보여주고 버튼을 감춘다', async () => {
    vi.mocked(auth.fetchMe).mockResolvedValue(anonymous)
    vi.mocked(auth.fetchLoginOptions).mockResolvedValue({
      googleEnabled: false,
      autoApproveSignup: false,
    })

    const wrapper = mount(App)
    await flushPromises()

    expect(wrapper.find('.google-button').exists()).toBe(false)
    expect(wrapper.text()).toContain('Google 로그인이 아직 설정되지 않았습니다.')
  })

  it('로그인하면 워크스페이스와 아바타를 보여준다', async () => {
    vi.mocked(auth.fetchMe).mockResolvedValue(member)

    const wrapper = mount(App)
    await flushPromises()

    expect(wrapper.find('.auth-card').exists()).toBe(false)
    expect(wrapper.find('.segmented').text()).toContain('기업')
    // 아바타는 이름 첫 글자, 툴팁은 이메일
    const avatar = wrapper.get('.avatar')
    expect(avatar.text()).toBe('홍')
    expect(avatar.attributes('title')).toBe('member@example.com')
  })

  it('아바타를 누르면 이름 수정 팝오버가 열리고 저장하면 me 가 갱신된다', async () => {
    vi.mocked(auth.fetchMe).mockResolvedValue(member)
    vi.mocked(auth.updateMyName).mockResolvedValue({ ...member, displayName: '김철수' })

    const wrapper = mount(App)
    await flushPromises()
    expect(wrapper.find('.popover').exists()).toBe(false)

    await wrapper.get('.avatar').trigger('click')
    expect(wrapper.find('.popover').exists()).toBe(true)
    expect(wrapper.find('.popover').text()).toContain('member@example.com')

    await wrapper.get('.popover input').setValue('김철수')
    await wrapper.get('.popover form').trigger('submit')
    await flushPromises()

    expect(auth.updateMyName).toHaveBeenCalledWith('김철수')
    expect(wrapper.find('.popover').exists()).toBe(false)
    expect(wrapper.get('.avatar').text()).toBe('김')
  })

  it('이름이 없으면 아바타에 이메일 첫 글자를 대문자로 보여준다', async () => {
    vi.mocked(auth.fetchMe).mockResolvedValue(admin)

    const wrapper = mount(App)
    await flushPromises()

    expect(wrapper.get('.avatar').text()).toBe('A')
  })

  /** 관리자는 성격이 달라 기업/채용공고 세그먼트에 넣지 않는다 — docs/design-system.md 규칙 1. */
  it('관리자 버튼은 세그먼트 밖에 있고 일반 계정에게는 보이지 않는다', async () => {
    vi.mocked(auth.fetchMe).mockResolvedValue(admin)
    const asAdmin = mount(App)
    await flushPromises()

    expect(asAdmin.find('.segmented').text()).not.toContain('관리자')
    expect(asAdmin.find('.topbar-actions').text()).toContain('관리자')

    vi.mocked(auth.fetchMe).mockResolvedValue(member)
    const asMember = mount(App)
    await flushPromises()

    expect(asMember.find('.topbar-actions').text()).not.toContain('관리자')
  })

  it('/postings 에서는 채용공고 보드를 보여준다', async () => {
    setPath(ROUTES.postings)
    vi.mocked(auth.fetchMe).mockResolvedValue(member)

    const wrapper = mount(App)
    await flushPromises()

    expect(wrapper.find('.page-head').text()).toContain('채용공고')
    expect(wrapper.find('.tabs').exists()).toBe(true)
  })

  /** 일반 계정에게는 관리자 링크도, /admin 화면도 주지 않는다. */
  it('일반 계정이 /admin 에 들어오면 권한 안내만 보여준다', async () => {
    setPath(ROUTES.admin)
    vi.mocked(auth.fetchMe).mockResolvedValue(member)

    const wrapper = mount(App)
    await flushPromises()

    expect(wrapper.text()).toContain('관리자 권한이 필요합니다.')
    expect(wrapper.find('.table-wrap').exists()).toBe(false)
    expect(wrapper.find('.admin-panel').exists()).toBe(false)
  })

  it('관리자 계정이 /admin 에 들어오면 관리자 콘솔을 보여준다', async () => {
    setPath(ROUTES.admin)
    vi.mocked(auth.fetchMe).mockResolvedValue(admin)

    const wrapper = mount(App)
    await flushPromises()

    expect(wrapper.find('.page-head').text()).toContain('관리자')
    expect(wrapper.find('.admin-panel').exists()).toBe(true)
  })

  /** Google 로그인 실패는 리다이렉트 쿼리로 돌아온다. */
  it('authError 쿼리를 사람이 읽을 문구로 바꿔 보여준다', async () => {
    setPath('/?authError=ACCOUNT_NOT_APPROVED')
    vi.mocked(auth.fetchMe).mockResolvedValue(anonymous)

    const wrapper = mount(App)
    await flushPromises()

    expect(wrapper.text()).toContain('관리자 승인 후 로그인할 수 있습니다.')
    expect(window.location.search).toBe('')
  })
})
