import { mount } from '@vue/test-utils'
import { afterEach, describe, expect, it, vi } from 'vitest'
import PostingForm from '../components/PostingForm.vue'
import { nowLocalInput } from '../types/posting'

describe('PostingForm', () => {
  afterEach(() => vi.useRealTimers())

  it('새 공고와 새 절차 일정은 폼을 연 현재 분이 기본값이다', async () => {
    vi.useFakeTimers()
    vi.setSystemTime(new Date(2026, 8, 6, 14, 37))
    const expected = nowLocalInput()
    const wrapper = mount(PostingForm, {
      props: { posting: null, companies: [], saving: false, apiFieldErrors: {} },
    })

    expect(wrapper.get<HTMLInputElement>('input[type="datetime-local"]').element.value).toBe(expected)
    await wrapper.findAll('.repeater > .button')[1].trigger('click')
    expect(wrapper.findAll<HTMLInputElement>('input[type="datetime-local"]')[1].element.value).toBe(expected)
  })

  it('세분화된 아홉 상태만 새 입력 선택지로 보여준다', () => {
    const wrapper = mount(PostingForm, {
      props: { posting: null, companies: [], saving: false, apiFieldErrors: {} },
    })
    const statusSelect = wrapper.findAll('select')[2]
    expect(statusSelect.findAll('option').map((option) => option.text())).toEqual([
      '관심', '작성 중', '제출 완료', '필기 준비', '면접 준비', '합격', '서류 탈락', '필기 탈락', '면접 탈락',
    ])
  })
})
