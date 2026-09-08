import { flushPromises, mount } from '@vue/test-utils'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import AdminView from '../components/AdminView.vue'
import * as api from '../api/admin'
import type { AdminUser, Me, UserDataCount } from '../types/auth'

vi.mock('../api/admin', () => ({
  listUsers: vi.fn(),
  listUserDataCounts: vi.fn(),
  fetchSettings: vi.fn(() => Promise.resolve({ autoApproveSignup: false, updatedAt: '2026-09-01T00:00:00Z' })),
  changeUserStatus: vi.fn(),
  changeUserRole: vi.fn(),
  changeUserName: vi.fn(),
  deleteUser: vi.fn(),
  updateAutoApproveSignup: vi.fn(),
}))

const admin: Me = {
  authenticated: true,
  id: '20000000-0000-0000-0000-0000000000ad',
  email: 'admin@example.com',
  displayName: null,
  role: 'ADMIN',
}

const alice: AdminUser = {
  id: '10000000-0000-0000-0000-000000000001',
  email: 'alice@example.com',
  displayName: '앨리스',
  role: 'USER',
  status: 'ACTIVE',
  googleLinked: true,
  createdAt: '2026-09-01T00:00:00Z',
  updatedAt: '2026-09-01T00:00:00Z',
}

/** 데이터가 하나도 없는 계정. 개수 응답에 안 들어온다. */
const bob: AdminUser = {
  ...alice,
  id: '10000000-0000-0000-0000-000000000002',
  email: 'bob@example.com',
  displayName: '밥',
}

const aliceCounts: UserDataCount = { userId: alice.id, companies: 3, postings: 12, positions: 18 }

function numbersOf(wrapper: ReturnType<typeof mount>, row: number) {
  return wrapper.findAll('tbody tr')[row].findAll('.data-table__num').map((cell) => cell.text())
}

describe('AdminView 계정별 등록 데이터 수', () => {
  beforeEach(() => {
    vi.mocked(api.listUsers).mockReset().mockResolvedValue([alice, bob])
    vi.mocked(api.listUserDataCounts).mockReset().mockResolvedValue([aliceCounts])
    vi.mocked(api.changeUserStatus).mockReset()
  })

  it('계정마다 기업·공고·직무 수를 보여주고 없는 계정은 0 으로 채운다', async () => {
    const wrapper = mount(AdminView, { props: { me: admin } })
    await flushPromises()

    expect(numbersOf(wrapper, 0)).toEqual(['3', '12', '18'])
    // 응답에 없던 계정은 빈칸이 아니라 0 이다.
    expect(numbersOf(wrapper, 1)).toEqual(['0', '0', '0'])
  })

  /** 개수는 userId 로 따로 들고 있다. 상태를 바꿔 행을 갈아끼워도 살아남아야 한다. */
  it('상태를 바꿔도 그 행의 개수가 사라지지 않는다', async () => {
    vi.mocked(api.changeUserStatus).mockResolvedValue({ ...alice, status: 'SUSPENDED' })
    const wrapper = mount(AdminView, { props: { me: admin } })
    await flushPromises()

    const suspend = wrapper.findAll('tbody tr')[0].findAll('button')
      .find((button) => button.text() === '정지')
    await suspend!.trigger('click')
    await flushPromises()

    expect(numbersOf(wrapper, 0)).toEqual(['3', '12', '18'])
  })

  /** 계정 관리가 개수보다 중요하다. 개수 조회가 실패해도 표는 떠야 한다. */
  it('개수 조회가 실패해도 계정 목록은 그대로 뜬다', async () => {
    vi.mocked(api.listUserDataCounts).mockRejectedValue(new Error('boom'))
    const wrapper = mount(AdminView, { props: { me: admin } })
    await flushPromises()

    expect(wrapper.findAll('tbody tr')).toHaveLength(2)
    expect(wrapper.text()).toContain('alice@example.com')
    expect(numbersOf(wrapper, 0)).toEqual(['0', '0', '0'])
  })
})
