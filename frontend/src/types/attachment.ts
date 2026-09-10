/** 첨부 메타. 이력서 행에는 id 문자열만 저장한다 — docs/attachments.md. */
export interface AttachmentMeta {
  id: string
  filename: string
  contentType: string
  size: number
}

/** 파일 선택 대화상자의 필터. 실제 판정은 서버가 파일 앞머리로 한다. */
export const ATTACHMENT_ACCEPT = 'image/jpeg,image/png,application/pdf,.jpg,.jpeg,.png,.pdf'

/** 10MB. 백엔드 AttachmentService.MAX_BYTES · nginx client_max_body_size 와 함께 움직인다. */
export const MAX_ATTACHMENT_BYTES = 10 * 1024 * 1024

export const isImage = (contentType: string) => contentType.startsWith('image/')

export function formatBytes(size: number): string {
  if (size < 1024) return `${size} B`
  if (size < 1024 * 1024) return `${Math.round(size / 1024)} KB`
  return `${(size / 1024 / 1024).toFixed(1)} MB`
}
