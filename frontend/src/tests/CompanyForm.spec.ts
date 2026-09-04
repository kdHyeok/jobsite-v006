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

  it('업종을 쉼표로 나누고 중복과 공백을 정리한다', async () => {
    const wrapper = mount(CompanyForm, {
      props: { company: null, saving: false, apiFieldErrors: {} },
    })

    await wrapper.get('input[autofocus]').setValue('테스트 기업')
    await wrapper.get('input[list="industry-suggestions"]').setValue(' IT서비스 , SI,IT서비스 , ')
    await wrapper.get('input[type="month"]').setValue('2015-03')
    await wrapper.get('form').trigger('submit')

    expect(wrapper.emitted('submit')?.[0]?.[0]).toMatchObject({
      name: '테스트 기업',
      industries: ['IT서비스', 'SI'],
      // input[type=month] 는 일자가 없다. 그 달 1일로 보낸다.
      foundedOn: '2015-03-01',
    })
  })
})
