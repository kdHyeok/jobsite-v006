import type { ApiErrorBody } from '../types/company'
import { API } from '../routes'

export class ApiClientError extends Error {
  public readonly status: number
  public readonly code: string
  public readonly fieldErrors: Record<string, string>

  constructor(
    status: number,
    code: string,
    message: string,
    fieldErrors: Record<string, string> = {},
  ) {
    super(message)
    this.name = 'ApiClientError'
    this.status = status
    this.code = code
    this.fieldErrors = fieldErrors
  }
}

const SAFE_METHODS = ['GET', 'HEAD', 'OPTIONS']

/** Spring Security가 내려준 XSRF-TOKEN 쿠키를 그대로 헤더로 돌려보낸다. */
function csrfToken(): string {
  const match = document.cookie.match(/(?:^|;\s*)XSRF-TOKEN=([^;]*)/)
  return match ? decodeURIComponent(match[1]) : ''
}

let refreshInFlight: Promise<boolean> | null = null

/** 여러 API가 동시에 401이어도 리프레시 토큰은 한 번만 회전한다. */
export function refreshSession(): Promise<boolean> {
  if (!refreshInFlight) {
    refreshInFlight = fetch(API.refresh, {
      method: 'POST',
      headers: { 'X-XSRF-TOKEN': csrfToken() },
      credentials: 'same-origin',
    })
      .then((response) => response.status === 204)
      .catch(() => false)
      .finally(() => { refreshInFlight = null })
  }
  return refreshInFlight
}

export async function request<T>(path: string, init?: RequestInit): Promise<T> {
  const method = (init?.method ?? 'GET').toUpperCase()

  function headers(): Record<string, string> {
    const value: Record<string, string> = { ...(init?.headers as Record<string, string>) }
    // URLSearchParams 본문은 fetch가 알아서 form-urlencoded 헤더를 붙인다.
    // FormData 도 마찬가지다 — 여기서 Content-Type 을 넣으면 boundary 가 빠져 서버가 파트를 못 읽는다.
    const selfTyped = init?.body instanceof URLSearchParams || init?.body instanceof FormData
    if (init?.body && !selfTyped && !value['Content-Type']) {
      value['Content-Type'] = 'application/json'
    }
    if (!SAFE_METHODS.includes(method)) {
      value['X-XSRF-TOKEN'] = csrfToken()
    }
    return value
  }

  const send = () => fetch(path, { ...init, headers: headers(), credentials: 'same-origin' })
  let response = await send()
  if (response.status === 401 && path !== API.refresh && await refreshSession()) {
    response = await send()
  }

  if (!response.ok) {
    let body: Partial<ApiErrorBody> = {}
    try {
      body = (await response.json()) as Partial<ApiErrorBody>
    } catch {
      // JSON이 아닌 proxy 오류도 사용자 메시지로 안전하게 변환한다.
    }
    throw new ApiClientError(
      response.status,
      body.code ?? 'REQUEST_FAILED',
      body.message ?? '요청을 처리하지 못했습니다.',
      body.fieldErrors ?? {},
    )
  }

  if (response.status === 204) {
    return undefined as T
  }
  const text = await response.text()
  return (text ? JSON.parse(text) : undefined) as T
}
