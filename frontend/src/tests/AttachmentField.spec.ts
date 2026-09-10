import { flushPromises, mount } from '@vue/test-utils'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import AttachmentField from '../components/AttachmentField.vue'
import * as api from '../api/attachments'
import { ApiClientError } from '../api/http'

vi.mock('../api/attachments', () => ({
  uploadAttachment: vi.fn(),
  fetchAttachmentMeta: vi.fn(),
  deleteAttachment: vi.fn(),
  attachmentUrl: (id: string) => `/api/attachments/${id}`,
}))

const NEW_ID = '11111111-1111-1111-1111-111111111111'
const OLD_ID = '22222222-2222-2222-2222-222222222222'

const meta = (id: string, filename = '정보처리기사.pdf') =>
  ({ id, filename, contentType: 'application/pdf', size: 20480 })

/** jsdom 의 file input 은 files 를 직접 못 넣는다. 선택된 상태를 흉내 낸다. */
async function choose(wrapper: ReturnType<typeof mount>, file: File) {
  const input = wrapper.find('input[type="file"]')
  Object.defineProperty(input.element, 'files', { value: [file], configurable: true })
  await input.trigger('change')
  await flushPromises()
}

const png = () => new File([new Uint8Array([0x89, 0x50, 0x4e, 0x47])], 'x.png', { type: 'image/png' })

describe('AttachmentField', () => {
  beforeEach(() => {
    vi.mocked(api.uploadAttachment).mockReset().mockResolvedValue(meta(NEW_ID))
    vi.mocked(api.fetchAttachmentMeta).mockReset().mockResolvedValue(meta(OLD_ID, '이전.pdf'))
    vi.mocked(api.deleteAttachment).mockReset().mockResolvedValue(undefined)
  })

  it('파일을 고르면 바로 올리고 새 id 를 알린다', async () => {
    const wrapper = mount(AttachmentField, { props: { id: '', label: '자격증 사본' } })
    await choose(wrapper, png())

    expect(api.uploadAttachment).toHaveBeenCalledOnce()
    expect(wrapper.emitted('update:id')?.[0]).toEqual([NEW_ID])
    expect(wrapper.text()).toContain('정보처리기사.pdf')
  })

  /** 교체는 새 업로드 + 이전 것 삭제다. 덮어쓰면 복제된 이력서의 첨부까지 바뀐다. */
  it('이미 첨부가 있으면 교체하면서 이전 파일을 지운다', async () => {
    const wrapper = mount(AttachmentField, { props: { id: OLD_ID, label: '졸업증' } })
    await flushPromises()
    await choose(wrapper, png())

    expect(wrapper.emitted('update:id')?.[0]).toEqual([NEW_ID])
    expect(api.deleteAttachment).toHaveBeenCalledWith(OLD_ID)
  })

  it('서버가 거부하면 이유를 보여주고 값을 바꾸지 않는다', async () => {
    vi.mocked(api.uploadAttachment).mockRejectedValue(
      new ApiClientError(415, 'UNSUPPORTED_FILE_TYPE', 'jpg, png, pdf 만 첨부할 수 있습니다.'))
    const wrapper = mount(AttachmentField, { props: { id: '', label: '이수증' } })
    await choose(wrapper, png())

    expect(wrapper.text()).toContain('jpg, png, pdf 만 첨부할 수 있습니다.')
    expect(wrapper.emitted('update:id')).toBeUndefined()
  })
})
