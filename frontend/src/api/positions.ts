import { request } from './http'
import { API } from '../routes'
import type { Position, PositionPayload } from '../types/position'

export { ApiClientError } from './http'

/** 직무는 공고 폼이 만든다. 여기엔 생성이 없다. */
export const listPositions = (q = '') =>
  request<Position[]>(q ? `${API.positions}?q=${encodeURIComponent(q)}` : API.positions)

export const getPosition = (id: string) => request<Position>(API.position(id))

export const updatePosition = (id: string, payload: PositionPayload) =>
  request<Position>(API.position(id), { method: 'PUT', body: JSON.stringify(payload) })

/** 참고 정보 연결을 집합으로 통째로 교체한다. */
export const replacePositionReferences = (id: string, referenceIds: string[]) =>
  request<Position>(API.positionReferences(id), { method: 'PUT', body: JSON.stringify({ referenceIds }) })

export const deletePosition = (id: string) =>
  request<void>(API.position(id), { method: 'DELETE' })
