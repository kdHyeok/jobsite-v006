import { mount } from '@vue/test-utils'
import { describe, expect, it } from 'vitest'
import CompanyForm from '../components/CompanyForm.vue'

describe('CompanyForm', () => {
  it('기업명이 없으면 저장하지 않는다', async () => {
    const wrapper = mount(CompanyForm, {
      props: { company: null, saving: false, apiFieldErrors: {} },
    })

    await wrapper.get('form').trigger('submit')

    expect(wrapper.emitted('submit')).toBeUndefined()
    expect(wrapper.text()).toContain('기업명을 입력해 주세요.')
  })

  it('유효한 값을 payload로 전달한다', async () => {
    const wrapper = mount(CompanyForm, {
      props: { company: null, saving: false, apiFieldErrors: {} },
    })

    await wrapper.get('input[autofocus]').setValue('테스트 기업')
    await wrapper.get('form').trigger('submit')

    expect(wrapper.emitted('submit')?.[0]?.[0]).toMatchObject({
      name: '테스트 기업', status: 'INTERESTED',
    })
  })
})
