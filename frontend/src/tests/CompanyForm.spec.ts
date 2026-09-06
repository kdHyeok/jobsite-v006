import { mount } from '@vue/test-utils'
import { describe, expect, it } from 'vitest'
import CompanyForm from '../components/CompanyForm.vue'

describe('CompanyForm', () => {
  it('기업명이 없으면 저장하지 않는다', async () => {
    const wrapper = mount(CompanyForm, {
      props: { company: null, saving: false, apiFieldErrors: {}, industryOptions: [] },
    })

    await wrapper.get('form').trigger('submit')

    expect(wrapper.emitted('submit')).toBeUndefined()
    expect(wrapper.text()).toContain('기업명을 입력해 주세요.')
  })

  it('업종 태그를 추가하고 매출 단위를 원으로 환산한다', async () => {
    const wrapper = mount(CompanyForm, {
      props: { company: null, saving: false, apiFieldErrors: {}, industryOptions: ['IT서비스', 'SI'] },
    })

    await wrapper.get('input[autofocus]').setValue('테스트 기업')
    const tagInput = wrapper.get('.tag-input input')
    await tagInput.setValue('IT서비스')
    await tagInput.trigger('keydown.enter')
    await wrapper.get('input[type="month"]').setValue('2015-03')
    await wrapper.get('[aria-label="매출액 단위"] button:last-child').trigger('click')
    await wrapper.get('input[type="number"]').setValue('12.5')
    await wrapper.findAll('textarea')[1].setValue('유연근무·교육비')
    await wrapper.get('form').trigger('submit')

    expect(wrapper.emitted('submit')?.[0]?.[0]).toMatchObject({
      name: '테스트 기업',
      industries: ['IT서비스'],
      annualRevenue: 1_250_000_000,
      revenueUnit: 'HUNDRED_MILLION',
      benefits: '유연근무·교육비',
      // input[type=month] 는 일자가 없다. 그 달 1일로 보낸다.
      foundedOn: '2015-03-01',
    })
  })
})
