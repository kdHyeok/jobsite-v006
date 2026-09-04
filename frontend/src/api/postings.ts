import { request } from './http'
import { API } from '../routes'
import type { ApplicationStage, JobPosting, JobPostingPayload } from '../types/posting'

export { ApiClientError } from './http'

/** 진행 중 공고를 마감 임박 순으로. 서버가 조회 시점에 마감된 관심 공고를 보관함으로 옮긴다. */
export const listPostings = () => request<JobPosting[]>(API.postings)

export const listArchivedPostings = () =>
  request<JobPosting[]>(`${API.postings}?archived=true`)

export const createPosting = (payload: JobPostingPayload) =>
  request<JobPosting>(API.postings, { method: 'POST', body: JSON.stringify(payload) })

export const updatePosting = (id: string, payload: JobPostingPayload) =>
  request<JobPosting>(API.posting(id), { method: 'PUT', body: JSON.stringify(payload) })

export const changePostingStage = (id: string, stage: ApplicationStage) =>
  request<JobPosting>(API.postingStage(id), { method: 'PATCH', body: JSON.stringify({ stage }) })

export const setPostingArchived = (id: string, archived: boolean) =>
  request<JobPosting>(`${API.postingArchive(id)}?archived=${archived}`, { method: 'PATCH' })

export const deletePosting = (id: string) =>
  request<void>(API.posting(id), { method: 'DELETE' })
