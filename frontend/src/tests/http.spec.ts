import { beforeEach, describe, expect, it, vi } from 'vitest'
import { request } from '../api/http'
import { fetchMe } from '../api/auth'
import { API } from '../routes'

describe('HTTP 세션 갱신', () => {
  beforeEach(() => {
    vi.restoreAllMocks()
    document.cookie = 'XSRF-TOKEN=test-csrf; path=/'
  })

  it('401이면 한 번 갱신하고 원래 요청을 재시도한다', async () => {
    const fetchMock = vi.fn()
      .mockResolvedValueOnce(new Response('{}', { status: 401 }))
      .mockResolvedValueOnce(new Response(null, { status: 204 }))
      .mockResolvedValueOnce(new Response('{"ok":true}', { status: 200 }))
    vi.stubGlobal('fetch', fetchMock)

    await expect(request<{ ok: boolean }>('/api/companies')).resolves.toEqual({ ok: true })
    expect(fetchMock.mock.calls.map(([path]) => path)).toEqual([
      '/api/companies',
      API.refresh,
      '/api/companies',
    ])
    expect(fetchMock.mock.calls[1][1].headers['X-XSRF-TOKEN']).toBe('test-csrf')
  })

  it('첫 me가 익명이면 갱신 후 로그인 계정을 다시 읽는다', async () => {
    const fetchMock = vi.fn()
      .mockResolvedValueOnce(new Response('{"authenticated":false}', { status: 200 }))
      .mockResolvedValueOnce(new Response(null, { status: 204 }))
      .mockResolvedValueOnce(new Response('{"authenticated":true,"id":"1"}', { status: 200 }))
    vi.stubGlobal('fetch', fetchMock)

    await expect(fetchMe()).resolves.toMatchObject({ authenticated: true, id: '1' })
    expect(fetchMock.mock.calls.map(([path]) => path)).toEqual([API.me, API.refresh, API.me])
  })
})
