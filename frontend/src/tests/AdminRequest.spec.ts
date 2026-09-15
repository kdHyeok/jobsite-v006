import { flushPromises, mount } from '@vue/test-utils'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import AdminRequestPanel from '../components/AdminRequestPanel.vue'
import AdminRequestWidget from '../components/AdminRequestWidget.vue'
import * as api from '../api/admin-requests'
import type { AdminRequest, AdminRequestForAdmin } from '../types/admin-request'

vi.mock('../api/admin-requests', () => ({
  listMyAdminRequests: vi.fn(),
  createAdminRequest: vi.fn(),
  updateAdminRequest: vi.fn(),
  deleteMyAdminRequest: vi.fn(),
  readAdminFeedback: vi.fn(),
  listAdminRequests: vi.fn(),
  saveAdminFeedback: vi.fn(),
  deleteAdminFeedback: vi.fn(),
  deleteAdminRequest: vi.fn(),
}))

const request: AdminRequest = {
  id: '10000000-0000-0000-0000-000000000001',
  kind: 'BUG_REPORT',
  message: '화면이 열리지 않습니다.',
  feedback: '새로 고침 후 다시 확인해 주세요.',
  feedbackUpdatedAt: '2026-09-15T03:01:00Z',
  feedbackUnread: true,
  createdAt: '2026-09-15T03:00:00Z',
  updatedAt: '2026-09-15T03:01:00Z',
}

describe('관리자 요청', () => {
  beforeEach(() => vi.clearAllMocks())

  it('사용자가 새 피드백을 열면 확인 상태로 저장한다', async () => {
    vi.mocked(api.listMyAdminRequests).mockResolvedValue([request])
    vi.mocked(api.readAdminFeedback).mockResolvedValue({ ...request, feedbackUnread: false })
    const wrapper = mount(AdminRequestWidget)

    await wrapper.get('.request-fab').trigger('click')
    await flushPromises()
    expect(wrapper.text()).toContain('새 피드백')

    await wrapper.get('.feedback-button').trigger('click')
    await flushPromises()
    expect(api.readAdminFeedback).toHaveBeenCalledWith(request.id)
    expect(wrapper.text()).toContain(request.feedback)
    expect(wrapper.text()).not.toContain('새 피드백')
  })

  it('관리자가 요청에 피드백을 작성한다', async () => {
    const item: AdminRequestForAdmin = {
      ...request, feedback: null, feedbackUpdatedAt: null, feedbackUnread: false,
      requesterId: '20000000-0000-0000-0000-000000000002',
      requesterEmail: 'user@example.com', requesterName: '사용자',
    }
    vi.mocked(api.listAdminRequests).mockResolvedValue([item])
    vi.mocked(api.saveAdminFeedback).mockResolvedValue({
      ...item, feedback: '수정했습니다. 다시 확인해 주세요.', feedbackUnread: true,
    })
    const wrapper = mount(AdminRequestPanel)
    await flushPromises()

    await wrapper.get('.admin-request-card button').trigger('click')
    await wrapper.get('textarea').setValue('수정했습니다. 다시 확인해 주세요.')
    await wrapper.get('form').trigger('submit')
    await flushPromises()

    expect(api.saveAdminFeedback).toHaveBeenCalledWith(item.id, '수정했습니다. 다시 확인해 주세요.')
    await wrapper.findAll('[role="tab"]')[1].trigger('click')
    expect(wrapper.text()).toContain('사용자 미확인')
  })
})
