import { describe, expect, it } from 'vitest'
import { buildZip } from '../utils/resume-export'

describe('이력서 첨부 ZIP', () => {
  it('빈 파일도 ZIP 시그니처와 종료 레코드를 만든다', () => {
    const bytes = buildZip([{ name: '증빙.pdf', bytes: new Uint8Array() }])
    expect([...bytes.slice(0, 4)]).toEqual([80, 75, 3, 4])
    expect([...bytes.slice(-22, -18)]).toEqual([80, 75, 5, 6])
  })
})
