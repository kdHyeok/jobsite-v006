export type AdminRequestKind = 'BUG_REPORT' | 'FEATURE_REQUEST'

export const adminRequestKindLabels: Record<AdminRequestKind, string> = {
  BUG_REPORT: '버그 제보',
  FEATURE_REQUEST: '기능 제안',
}

export type AdminRequest = {
  id: string
  kind: AdminRequestKind
  message: string
  feedback: string | null
  feedbackUpdatedAt: string | null
  feedbackUnread: boolean
  createdAt: string
  updatedAt: string
}

export type AdminRequestForAdmin = AdminRequest & {
  requesterId: string
  requesterEmail: string
  requesterName: string | null
}

export type AdminRequestInput = { kind: AdminRequestKind; message: string }
