import type { ApiErrorBody } from '../types/company'

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

export async function request<T>(path: string, init?: RequestInit): Promise<T> {
  const method = (init?.method ?? 'GET').toUpperCase()
  const headers: Record<string, string> = { ...(init?.headers as Record<string, string>) }

  // URLSearchParams 본문은 fetch가 알아서 form-urlencoded 헤더를 붙인다.
  if (init?.body && !(init.body instanceof URLSearchParams) && !headers['Content-Type']) {
    headers['Content-Type'] = 'application/json'
  }
  if (!SAFE_METHODS.includes(method)) {
    headers['X-XSRF-TOKEN'] = csrfToken()
  }

  const response = await fetch(path, { ...init, headers, credentials: 'same-origin' })

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
