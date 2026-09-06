import { mount } from '@vue/test-utils'
import { describe, expect, it } from 'vitest'
import TagInput from '../components/TagInput.vue'

describe('TagInput', () => {
  it('기존 태그를 고르고, 이름 수정과 삭제를 요청할 수 있다', async () => {
    const wrapper = mount(TagInput, {
      props: { modelValue: ['IT서비스'], suggestions: ['IT서비스', '금융권'] },
    })

    expect(wrapper.get('.tag-chip').text()).toContain('IT서비스')
    await wrapper.get('.tag-input input').trigger('focus')
    await wrapper.get('.tag-input__menu button').trigger('mousedown')
    expect(wrapper.emitted('update:modelValue')?.[0]).toEqual([['IT서비스', '금융권']])

    await wrapper.get('[aria-label="IT서비스 이름 수정"]').trigger('click')
    expect(wrapper.get<HTMLInputElement>('.tag-input input').element.value).toBe('IT서비스')
    expect(wrapper.emitted('update:modelValue')?.[1]).toEqual([[]])

    await wrapper.get('[aria-label="IT서비스 삭제"]').trigger('click')
    expect(wrapper.emitted('update:modelValue')?.[2]).toEqual([[]])
  })
})
