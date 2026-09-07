import { request } from './http'
import { API } from '../routes'
import type { Resume, ResumePayload, ResumeSummary } from '../types/resume'

export { ApiClientError } from './http'

/** 목록엔 content 가 없다. 카드는 이름·수정일만 그린다. */
export const listResumes = () => request<ResumeSummary[]>(API.resumes)

export const getResume = (id: string) => request<Resume>(API.resume(id))

/** content 를 비우면 서버가 빈 문서로 만든다. */
export const createResume = (name: string) =>
  request<Resume>(API.resumes, { method: 'POST', body: JSON.stringify({ name, content: null }) })

/** 문서 통째 교체. 행 단위 API 는 없다. */
export const updateResume = (id: string, payload: ResumePayload) =>
  request<Resume>(API.resume(id), { method: 'PUT', body: JSON.stringify(payload) })

export const copyResume = (id: string, name: string) =>
  request<Resume>(API.resumeCopy(id), { method: 'POST', body: JSON.stringify({ name }) })

export const deleteResume = (id: string) =>
  request<void>(API.resume(id), { method: 'DELETE' })
