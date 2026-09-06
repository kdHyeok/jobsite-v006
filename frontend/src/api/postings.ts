import { request } from './http'
import { API } from '../routes'
import type { ApplicationStatus, JobPosting, JobPostingPayload, StepResult } from '../types/posting'

export { ApiClientError } from './http'

/** 진행 중 공고를 마감 임박 순으로. 서버가 조회 시점에 마감된 관심·작성중 공고를 보관함으로 옮긴다. */
export const listPostings = () => request<JobPosting[]>(API.postings)

/** 목록에 없는 공고(보관됨)로 건너뛸 때 쓴다. */
export const getPosting = (id: string) => request<JobPosting>(API.posting(id))

export const listArchivedPostings = () =>
  request<JobPosting[]>(`${API.postings}?archived=true`)

export const createPosting = (payload: JobPostingPayload) =>
  request<JobPosting>(API.postings, { method: 'POST', body: JSON.stringify(payload) })

export const updatePosting = (id: string, payload: JobPostingPayload) =>
  request<JobPosting>(API.posting(id), { method: 'PUT', body: JSON.stringify(payload) })

export const changePostingStatus = (id: string, status: ApplicationStatus) =>
  request<JobPosting>(API.postingStatus(id), { method: 'PATCH', body: JSON.stringify({ status }) })

export const changeStepResult = (id: string, seq: number, result: StepResult) =>
  request<JobPosting>(API.postingStep(id, seq), { method: 'PATCH', body: JSON.stringify({ result }) })

export const setPostingArchived = (id: string, archived: boolean) =>
  request<JobPosting>(`${API.postingArchive(id)}?archived=${archived}`, { method: 'PATCH' })

export const deletePosting = (id: string) =>
  request<void>(API.posting(id), { method: 'DELETE' })
