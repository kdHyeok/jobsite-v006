import { mount } from '@vue/test-utils'
import { describe, expect, it } from 'vitest'
import MarkdownMemo from '../components/MarkdownMemo.vue'
import { indentSelection, parseMarkdown } from '../utils/markdown'

describe('Markdown memo', () => {
  it('제목·목록·들여쓰기와 안전한 새 탭 링크를 렌더링한다', () => {
    const wrapper = mount(MarkdownMemo, {
      props: {
        source: '# 제목\n###### 작은 제목\n- 항목\n  - 하위\n1. 첫째\n2. 둘째\n[이름](https://example.com/a) https://example.org/b.\n<script>alert(1)</script>',
      },
    })

    expect(wrapper.get('h1').text()).toBe('제목')
    expect(wrapper.get('h6').text()).toBe('작은 제목')
    expect(wrapper.findAll('ul li')).toHaveLength(2)
    expect(wrapper.findAll('ol li')).toHaveLength(2)
    expect(wrapper.findAll('a').map((link) => [link.text(), link.attributes('href')])).toEqual([
      ['이름', 'https://example.com/a'],
      ['https://example.org/b', 'https://example.org/b'],
    ])
    expect(wrapper.findAll('a').every((link) => link.attributes('target') === '_blank')).toBe(true)
    expect(wrapper.find('script').exists()).toBe(false)
  })

  it('Tab과 Shift+Tab으로 선택한 줄의 들여쓰기를 바꾼다', () => {
    const indented = indentSelection('- 하나\n- 둘', 0, 7, false)
    expect(indented.value).toBe('  - 하나\n  - 둘')
    expect(indentSelection(indented.value, 0, indented.value.length, true).value).toBe('- 하나\n- 둘')
    expect(parseMarkdown(indented.value)[0]).toMatchObject({
      kind: 'ul',
      items: [{ indent: 1 }, { indent: 1 }],
    })
  })
})
